package lk.tideline.cleanup.service;

import lk.tideline.cleanup.config.TidelineProperties;
import lk.tideline.cleanup.dto.ProjectDtos.ParticipantResponse;
import lk.tideline.cleanup.dto.ProjectDtos.ProjectResponse;
import lk.tideline.cleanup.dto.ProjectDtos.ProjectUpdateRequest;
import lk.tideline.cleanup.dto.ProjectDtos.ResourcesRequest;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.CleanupProjectRepository;
import lk.tideline.cleanup.repository.ProjectParticipantRepository;
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
    private final AlertService alertService;
    private final TidelineProperties properties;

    public ProjectService(CleanupProjectRepository projectRepository,
                          ProjectParticipantRepository participantRepository,
                          AlertService alertService,
                          TidelineProperties properties) {
        this.projectRepository = projectRepository;
        this.participantRepository = participantRepository;
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
        }
        return toResponse(project, officer);
    }

    /**
     * The officer's approval carries the resources, so a project is created ready to run. The owner
     * hears about the project and what it needs in one alert, which the approval sends itself.
     */
    @Transactional
    public void assignAtApproval(CleanupProject project, ResourcesRequest request, User officer) {
        apply(project, request, officer);
    }

    private void apply(CleanupProject project, ResourcesRequest request, User officer) {
        List<EquipmentItem> equipment = request.equipment() == null ? List.of() : request.equipment().stream()
                .map(line -> new EquipmentItem(line.name().trim(), line.quantity()))
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

        if (project.getStatus() == ProjectStatus.PLANNED) {
            project.setStatus(ProjectStatus.ACTIVE);
            project.setStartedAt(Instant.now());
        }

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
