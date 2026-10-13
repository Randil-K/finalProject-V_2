package lk.tideline.cleanup.service;

import lk.tideline.cleanup.dto.OpportunityDtos.ApplicationResponse;
import lk.tideline.cleanup.dto.OpportunityDtos.ApplyRequest;
import lk.tideline.cleanup.dto.OpportunityDtos.CreateOpportunityRequest;
import lk.tideline.cleanup.dto.OpportunityDtos.OpportunityResponse;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.OpportunityApplicationRepository;
import lk.tideline.cleanup.repository.OpportunityRepository;
import lk.tideline.cleanup.repository.ProjectParticipantRepository;
import lk.tideline.cleanup.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityApplicationRepository applicationRepository;
    private final ProjectParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              OpportunityApplicationRepository applicationRepository,
                              ProjectParticipantRepository participantRepository,
                              UserRepository userRepository,
                              AlertService alertService) {
        this.opportunityRepository = opportunityRepository;
        this.applicationRepository = applicationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.alertService = alertService;
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> listOpen() {
        return opportunityRepository.findByOpenTrueOrderByCreatedAtDescIdDesc().stream()
                .map(OpportunityResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OpportunityResponse view(Long id) {
        return OpportunityResponse.from(get(id));
    }

    private Opportunity get(Long id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Opportunity " + id + " was not found."));
    }

    private ApplicationResponse withRecord(OpportunityApplication application) {
        Double average = participantRepository.averageMark(application.getDiver().getId());
        return ApplicationResponse.from(application, average == null ? null : Math.round(average * 10) / 10.0);
    }

    @Transactional
    public OpportunityResponse create(CreateOpportunityRequest request, User organization) {
        Opportunity opportunity = new Opportunity();
        opportunity.setOrganization(organization);
        opportunity.setTitle(request.title());
        opportunity.setDescription(request.description());
        opportunity.setRegion(request.region());
        opportunity.setRequiredCertification(request.requiredCertification());
        opportunity.setPaid(request.paid());

        Opportunity saved = opportunityRepository.save(opportunity);
        notifyMatchingDivers(saved);
        return OpportunityResponse.from(saved);
    }

    /** Divers whose preferred region matches are told about a new assignment. */
    private void notifyMatchingDivers(Opportunity opportunity) {
        String organisationName = opportunity.getOrganization().getOrganizationName() != null
                ? opportunity.getOrganization().getOrganizationName()
                : opportunity.getOrganization().getFullName();

        for (User diver : userRepository.findByRole(Role.DIVER)) {
            DiverProfile profile = diver.getDiverProfile();
            if (profile == null || !diver.isAvailableForAlerts()) {
                continue;
            }
            boolean matchesRegion = profile.getPreferredRegions().isEmpty()
                    || profile.getPreferredRegions().stream()
                    .anyMatch(region -> region.equalsIgnoreCase(opportunity.getRegion()));
            if (matchesRegion) {
                alertService.send(diver, AlertType.OPPORTUNITY,
                        "New opportunity matches your certification",
                        organisationName + " posted " + opportunity.getTitle()
                                + " in " + opportunity.getRegion() + ".",
                        null, null, null);
            }
        }
    }

    @Transactional
    public ApplicationResponse apply(Long opportunityId, User diver, ApplyRequest request) {
        Opportunity opportunity = get(opportunityId);

        if (!opportunity.isOpen()) {
            throw new IllegalStateException("This opportunity is closed.");
        }

        applicationRepository.findByOpportunityAndDiver(opportunity, diver).ifPresent(existing -> {
            throw new IllegalStateException("You have already applied for this opportunity.");
        });

        OpportunityApplication application = new OpportunityApplication();
        application.setOpportunity(opportunity);
        application.setDiver(diver);
        application.setMessage(request == null ? null : request.message());
        return ApplicationResponse.from(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> applications(Long opportunityId, User organization) {
        Opportunity opportunity = get(opportunityId);
        if (!Objects.equals(opportunity.getOrganization().getId(), organization.getId())
                && organization.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only the posting organisation can see these applications.");
        }
        return applicationRepository.findByOpportunity(opportunity).stream()
                .map(this::withRecord)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> myApplications(User diver) {
        return applicationRepository.findByDiverOrderByCreatedAtDescIdDesc(diver).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    @Transactional
    public ApplicationResponse decide(Long applicationId, ApplicationStatus status, User organization) {
        OpportunityApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application " + applicationId + " was not found."));

        if (!Objects.equals(application.getOpportunity().getOrganization().getId(), organization.getId())
                && organization.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only the posting organisation can decide on this application.");
        }

        application.setStatus(status);
        alertService.send(application.getDiver(), AlertType.OPPORTUNITY,
                status == ApplicationStatus.ACCEPTED ? "Your application was accepted" : "Application update",
                application.getOpportunity().getTitle() + " — your application is now "
                        + status.name().toLowerCase() + ".",
                null, null, null);

        return withRecord(application);
    }
}
