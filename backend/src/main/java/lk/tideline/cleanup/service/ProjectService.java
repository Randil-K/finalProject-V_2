package lk.tideline.cleanup.service;

import lk.tideline.cleanup.config.TidelineProperties;
import lk.tideline.cleanup.dto.ProjectDtos.EquipmentLine;
import lk.tideline.cleanup.dto.ProjectDtos.ParticipantResponse;
import lk.tideline.cleanup.dto.ProjectDtos.ProjectResponse;
import lk.tideline.cleanup.dto.ProjectDtos.ProjectUpdateRequest;
import lk.tideline.cleanup.dto.ProjectDtos.ResourcesRequest;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.AlertRepository;
import lk.tideline.cleanup.repository.CleanupProjectRepository;
import lk.tideline.cleanup.repository.ProjectParticipantRepository;
import lk.tideline.cleanup.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectService {

    private final CleanupProjectRepository projectRepository;
    private final ProjectParticipantRepository participantRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final AlertService alertService;
    private final TidelineProperties properties;

    public ProjectService(CleanupProjectRepository projectRepository,
                          ProjectParticipantRepository participantRepository,
                          AlertRepository alertRepository,
                          UserRepository userRepository,
                          AlertService alertService,
                          TidelineProperties properties) {
        this.projectRepository = projectRepository;
        this.participantRepository = participantRepository;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.alertService = alertService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(ProjectStatus status, Long reportId, User viewer) {
        List<CleanupProject> projects = reportId != null
                ? projectRepository.findByReportIdOrderByCreatedAtDesc(reportId)
                : status != null
                    ? projectRepository.findByStatusOrderByCreatedAtDesc(status)
                    : projectRepository.findAllByOrderByCreatedAtDesc();
        return projects.stream()
                .filter(project -> status == null || project.getStatus() == status)
                .map(project -> toResponse(project, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse view(Long id, User viewer) {
        return toResponse(get(id), viewer);
    }

    private CleanupProject get(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project " + id + " was not found."));
    }

    private ProjectResponse toResponse(CleanupProject project, User viewer) {
        Boolean joined = viewer == null
                ? null
                : participantRepository.findByProjectAndUser(project, viewer).isPresent();

        // Participants' names are shown only to the people running the cleanup.
        boolean manages = viewer != null
                && (Objects.equals(project.getOwner().getId(), viewer.getId())
                    || viewer.getRole() == Role.ADMIN
                    || viewer.getRole() == Role.AUTHORITY);
        List<ParticipantResponse> participants = manages
                ? participantRepository.findByProjectOrderByJoinedAtAsc(project).stream()
                        .map(ParticipantResponse::from)
                        .toList()
                : null;

        boolean official = viewer != null && (viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.AUTHORITY);
        return ProjectResponse.from(project,
                participantRepository.countByProjectAndParticipantRole(project, ParticipantRole.VOLUNTEER),
                participantRepository.countByProjectAndParticipantRole(project, ParticipantRole.DIVER),
                joined,
                participants,
                official);
    }

    /**
     * The government officer revises the volunteers, divers and equipment they committed when approving.
     * Saving without {@code publish} keeps a draft; finalizing publishes it on the project.
     */
    @Transactional
    public ProjectResponse updateResources(Long projectId, ResourcesRequest request, User officer) {
        if (officer.getRole() != Role.AUTHORITY) {
            throw new AccessDeniedException("Only government officers assign project resources.");
        }
        CleanupProject project = get(projectId);
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw new IllegalStateException("This project is already complete.");
        }

        boolean firstFinalize = request.publish() && project.getResourcesFinalizedAt() == null;
        apply(project, request, officer);

        if (firstFinalize) {
            alertService.send(project.getOwner(), AlertType.RESOURCES_ASSIGNED,
                    "Resources assigned to " + project.getReference(),
                    "The government officer set out what " + project.getReference() + " needs: "
                            + describeResources(project) + ".",
                    null, project.getId(), null);
            callForHelp(project);
        }
        // A revision can bring the target down to what people have already pledged.
        checkGathered(project);
        return toResponse(project, officer);
    }

    /**
     * Module 6 — asks everyone living within the first radius of the site to join. Widening the call
     * later is {@link #widenCallForHelp}, which skips the people this round already reached.
     */
    private void callForHelp(CleanupProject project) {
        double radius = properties.getAlerts().getRecruitmentRadiusKm();
        project.setRecruitmentRadiusKm(radius);
        alertService.notifyProjectNearby(project, radius, AlertType.HELP_NEEDED,
                "Help needed at " + project.getLocationName(),
                project.getTitle() + " needs " + describeResources(project)
                        + ". Join if you can help, or ignore this if you cannot.",
                Set.of(project.getOwner().getId()));
    }

    /**
     * Widens a call that has waited long enough without filling. Everyone already asked is skipped,
     * so nobody hears about the same cleanup twice.
     */
    @Transactional
    public int widenCallForHelp(Long projectId) {
        CleanupProject project = get(projectId);
        double radius = properties.getAlerts().getWidenedRadiusKm();
        Set<Long> alreadyAsked = new java.util.HashSet<>();
        alreadyAsked.add(project.getOwner().getId());
        alertRepository.findByProjectIdAndType(project.getId(), AlertType.HELP_NEEDED)
                .forEach(alert -> alreadyAsked.add(alert.getRecipient().getId()));

        int sent = alertService.notifyProjectNearby(project, radius, AlertType.HELP_NEEDED,
                "Still looking for help at " + project.getLocationName(),
                project.getTitle() + " still needs " + describeShortfall(project)
                        + ". Join if you can help, or ignore this if you cannot.",
                alreadyAsked);

        project.setRecruitmentRadiusKm(radius);
        project.setRecruitmentWidenedAt(Instant.now());
        projectRepository.save(project);
        return sent;
    }

    /** Someone answers a call for help: they join the cleanup, or they let it pass. */
    @Transactional
    public ProjectResponse respondToCall(Long projectId, User user, boolean joining,
                                         List<EquipmentLine> pledged) {
        CleanupProject project = get(projectId);
        if (!joining) {
            return toResponse(project, user);
        }

        boolean alreadyIn = Objects.equals(project.getOwner().getId(), user.getId())
                || participantRepository.findByProjectAndUser(project, user).isPresent();
        if (!alreadyIn) {
            join(projectId, user, null);
            project = get(projectId);
        }
        pledgeEquipment(project, pledged);
        checkGathered(project);
        return toResponse(project, user);
    }

    /** Adds what someone promised to bring, never counting more than the cleanup asked for. */
    private void pledgeEquipment(CleanupProject project, List<EquipmentLine> pledged) {
        if (pledged == null || pledged.isEmpty()) {
            return;
        }
        for (EquipmentLine line : pledged) {
            if (line.quantity() <= 0) {
                continue;
            }
            project.getEquipment().stream()
                    .filter(item -> item.getName().equalsIgnoreCase(line.name().trim()))
                    .findFirst()
                    .ifPresent(item -> item.setSecuredQuantity(
                            Math.min(item.getQuantity(), item.getSecuredQuantity() + line.quantity())));
        }
        projectRepository.save(project);
    }

    /** Tells the administrators and the owner the first time a cleanup has everything it needs. */
    private void checkGathered(CleanupProject project) {
        if (project.getResourcesGatheredAt() != null || !hasEverythingItNeeds(project)) {
            return;
        }
        project.setResourcesGatheredAt(Instant.now());
        projectRepository.save(project);

        String body = project.getReference() + " has everyone and everything it asked for: "
                + describeResources(project) + ". It is ready to run.";
        alertService.send(project.getOwner(), AlertType.RESOURCES_GATHERED,
                "Your cleanup has everything it needs", body, null, project.getId(), null);
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            alertService.send(admin, AlertType.RESOURCES_GATHERED,
                    "Resources gathered for " + project.getReference(), body, null, project.getId(), null);
        }
    }

    /** Enough people have joined and every piece of equipment has been promised. */
    public boolean hasEverythingItNeeds(CleanupProject project) {
        long volunteers = participantRepository.countByProjectAndParticipantRole(project, ParticipantRole.VOLUNTEER);
        long divers = participantRepository.countByProjectAndParticipantRole(project, ParticipantRole.DIVER);
        return volunteers >= orZero(project.getVolunteersNeeded())
                && divers >= orZero(project.getDiversNeeded())
                && project.getEquipment().stream().allMatch(EquipmentItem::isSecured);
    }

    /** "8 volunteers, 1 diver" — what is still missing, for the widened call. */
    private String describeShortfall(CleanupProject project) {
        long volunteers = orZero(project.getVolunteersNeeded())
                - participantRepository.countByProjectAndParticipantRole(project, ParticipantRole.VOLUNTEER);
        long divers = orZero(project.getDiversNeeded())
                - participantRepository.countByProjectAndParticipantRole(project, ParticipantRole.DIVER);
        List<String> parts = new java.util.ArrayList<>();
        if (volunteers > 0) parts.add(volunteers + (volunteers == 1 ? " volunteer" : " volunteers"));
        if (divers > 0) parts.add(divers + (divers == 1 ? " diver" : " divers"));
        long equipment = project.getEquipment().stream().filter(item -> !item.isSecured()).count();
        if (equipment > 0) parts.add(equipment + (equipment == 1 ? " type of equipment" : " types of equipment"));
        return parts.isEmpty() ? describeResources(project) : String.join(", ", parts);
    }

    private static int securedSoFar(CleanupProject project, String name) {
        return project.getEquipment().stream()
                .filter(item -> item.getName().equalsIgnoreCase(name))
                .mapToInt(EquipmentItem::getSecuredQuantity)
                .findFirst()
                .orElse(0);
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private void apply(CleanupProject project, ResourcesRequest request, User officer) {
        // Revising the plan keeps what people have already promised to bring.
        List<EquipmentItem> equipment = request.equipment() == null ? List.of() : request.equipment().stream()
                .map(line -> new EquipmentItem(line.name().trim(), line.quantity(),
                        Math.min(line.quantity(), securedSoFar(project, line.name().trim()))))
                .toList();
        int volunteers = request.volunteersNeeded() == null ? 0 : request.volunteersNeeded();
        int divers = request.diversNeeded() == null ? 0 : request.diversNeeded();
        if (request.publish() && volunteers == 0 && divers == 0 && equipment.isEmpty()) {
            throw new IllegalArgumentException("Add the volunteers, divers or equipment this project needs before finalizing.");
        }

        project.setVolunteersNeeded(volunteers);
        project.setDiversNeeded(divers);
        project.getEquipment().clear();
        project.getEquipment().addAll(equipment);

        if (request.publish()) {
            project.setResourcesFinalizedAt(Instant.now());
            project.setResourcesFinalizedBy(officer);
        }
        projectRepository.save(project);
    }

    /** "20 volunteers, 3 divers, 2 types of equipment" — used in the alerts about a project's resources. */
    public static String describeResources(CleanupProject project) {
        int volunteers = project.getVolunteersNeeded() == null ? 0 : project.getVolunteersNeeded();
        int divers = project.getDiversNeeded() == null ? 0 : project.getDiversNeeded();
        int equipmentLines = project.getEquipment().size();
        List<String> parts = new java.util.ArrayList<>();
        if (volunteers > 0) parts.add(volunteers + (volunteers == 1 ? " volunteer" : " volunteers"));
        if (divers > 0) parts.add(divers + (divers == 1 ? " diver" : " divers"));
        if (equipmentLines > 0) parts.add(equipmentLines + (equipmentLines == 1 ? " type of equipment" : " types of equipment"));
        return String.join(", ", parts);
    }

    /**
     * Called when the government authority approves a report: the report becomes a cleanup project,
     * and the person who reported the site is its project owner.
     */
    @Transactional
    public CleanupProject createFromApprovedReport(PollutionReport report) {
        if (projectRepository.existsByReportId(report.getId())) {
            throw new IllegalStateException("A project already exists for this report.");
        }

        CleanupProject project = new CleanupProject();
        project.setReference("TMP-" + UUID.randomUUID());
        project.setTitle(report.getTitle());
        project.setDescription(report.getDescription());
        project.setReport(report);
        project.setOwner(report.getReporter());
        project.setLocationName(report.getLocationName());
        project.setProvince(report.getProvince());
        project.setLatitude(report.getLatitude());
        project.setLongitude(report.getLongitude());
        project.setStatus(ProjectStatus.PLANNED);

        CleanupProject saved = projectRepository.saveAndFlush(project);
        saved.setReference("CP-" + (100 + saved.getId()));

        alertService.notifyProjectNearby(saved, properties.getAlerts().getInitialRadiusKm(),
                "New cleanup project near " + saved.getLocationName(),
                saved.getTitle() + " — approved by the government officer. Join if you can help.",
                Set.of(report.getReporter().getId()));

        return saved;
    }

    @Transactional
    public ProjectResponse join(Long projectId, User user, ParticipantRole role) {
        CleanupProject project = get(projectId);

        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw new IllegalStateException("This cleanup is already complete.");
        }
        if (Objects.equals(project.getOwner().getId(), user.getId())) {
            throw new IllegalStateException("You are this project's owner, so you are already part of it.");
        }
        participantRepository.findByProjectAndUser(project, user).ifPresent(existing -> {
            throw new IllegalStateException("You have already joined this cleanup.");
        });

        ParticipantRole resolved = role != null ? role
                : (user.getRole() == Role.DIVER ? ParticipantRole.DIVER : ParticipantRole.VOLUNTEER);

        ProjectParticipant participant = new ProjectParticipant();
        participant.setProject(project);
        participant.setUser(user);
        participant.setParticipantRole(resolved);
        participantRepository.saveAndFlush(participant);
        // The cleanup is under way once the owner records progress, not when the first person signs up.
        checkGathered(project);

        return toResponse(project, user);
    }

    /** Module 8 — the project owner rates each participant (1-5) once the cleanup is complete. */
    @Transactional
    public ProjectResponse mark(Long projectId, Long participantId, int mark, User owner) {
        CleanupProject project = get(projectId);

        if (!Objects.equals(project.getOwner().getId(), owner.getId())) {
            throw new IllegalStateException("Only the project owner can rate contributions.");
        }
        if (project.getStatus() != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("Contributions can be rated once the cleanup is complete.");
        }

        ProjectParticipant participant = participantRepository.findById(participantId)
                .filter(p -> Objects.equals(p.getProject().getId(), project.getId()))
                .orElseThrow(() -> new NotFoundException("That person is not part of this cleanup."));

        boolean firstRating = participant.getContributionMark() == null;
        participant.setContributionMark(mark);
        participantRepository.saveAndFlush(participant);

        if (firstRating) {
            alertService.send(participant.getUser(), AlertType.PROJECT_UPDATE,
                    "Your contribution was rated",
                    owner.getFullName() + " rated your part in " + project.getTitle() + " " + mark
                            + " out of 5. It now shows on your profile.",
                    null, project.getId(), null);
        }

        return toResponse(project, owner);
    }

    /** Module 7 — progress evidence, completion percentage and recorded outcome. */
    @Transactional
    public ProjectResponse addUpdate(Long projectId, ProjectUpdateRequest request, User author) {
        CleanupProject project = get(projectId);

        // Progress is the project owner's responsibility; administrators and officers only follow it.
        if (!Objects.equals(project.getOwner().getId(), author.getId())) {
            throw new AccessDeniedException("Only the project owner can post progress updates.");
        }

        ProjectUpdate update = new ProjectUpdate();
        update.setProject(project);
        update.setAuthor(author);
        update.setStage(request.stage());
        update.setNote(request.note());
        update.setImageUrl(request.imageUrl());
        update.setCompletionPercentage(request.completionPercentage());
        project.getUpdates().add(update);

        if (request.completionPercentage() != null) {
            project.setCompletionPercentage(request.completionPercentage());
        }
        if (request.debrisRemovedKg() != null) {
            project.setDebrisRemovedKg(request.debrisRemovedKg());
        }
        if (project.getStatus() == ProjectStatus.PLANNED) {
            project.setStatus(ProjectStatus.ACTIVE);
            project.setStartedAt(Instant.now());
        }

        if (project.getCompletionPercentage() >= 100) {
            complete(project);
        }

        return toResponse(project, author);
    }

    private void complete(CleanupProject project) {
        project.setStatus(ProjectStatus.COMPLETED);
        project.setCompletionPercentage(100);
        project.setCompletedAt(Instant.now());

        PollutionReport report = project.getReport();
        if (report != null) {
            report.setStatus(ReportStatus.CLEANED);
            report.setUpdatedAt(Instant.now());
        }

        alertService.send(project.getOwner(), AlertType.PROJECT_UPDATE,
                "Your cleanup is complete",
                project.getReference() + " — " + project.getTitle() + " is marked complete"
                        + (project.getDebrisRemovedKg() != null
                                ? ", with " + project.getDebrisRemovedKg() + " kg of debris recorded." : ".")
                        + " The site you reported is now marked cleaned.",
                null, project.getId(), null);

        for (ProjectParticipant participant : project.getParticipants()) {
            DiverProfile profile = participant.getUser().getDiverProfile();
            if (profile != null) {
                profile.setCompletedProjects(profile.getCompletedProjects() + 1);
            }
            alertService.send(participant.getUser(), AlertType.PROJECT_UPDATE,
                    "Cleanup complete",
                    project.getTitle() + " is finished. Thank you for taking part.",
                    null, project.getId(), null);
        }
    }
}
