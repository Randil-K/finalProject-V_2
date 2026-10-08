package lk.tideline.cleanup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lk.tideline.cleanup.model.CleanupProject;
import lk.tideline.cleanup.model.ParticipantRole;
import lk.tideline.cleanup.model.ProjectParticipant;
import lk.tideline.cleanup.model.ProjectStatus;
import lk.tideline.cleanup.model.Severity;
import lk.tideline.cleanup.model.ProjectUpdate;
import lk.tideline.cleanup.model.UpdateStage;

import java.time.Instant;
import java.util.List;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record ProjectResponse(
            Long id,
            String reference,
            String title,
            String description,
            ProjectStatus status,
            int completionPercentage,
            String locationName,
            String province,
            Double latitude,
            Double longitude,
            Double debrisRemovedKg,
            Long reportId,
            /** How bad the reported site was, carried over from the report. */
            Severity severity,
            /** Photos and video of the site, carried over from the report that started it. */
            List<ReportDtos.EvidenceResponse> evidence,
            UserDtos.UserSummary owner,
            long volunteerCount,
            long diverCount,
            List<ProjectUpdateResponse> updates,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            /** Whether the signed-in viewer has joined; null for anonymous viewers. */
            Boolean joined,
            /** Only for the organiser, administrators and authority officers; null for everyone else. */
            List<ParticipantResponse> participants,
            /** The assigned resources: everyone once finalized, administrators and officers while a draft. */
            ResourcesResponse resources,
            /** The government officer's approval note, for administrators and officers only. */
            ApprovalNote approval,
            /** How far the call for help has reached, in kilometres; null until the resources are set. */
            Double recruitmentRadiusKm,
            /** When the call was widened because too few people had joined. */
            Instant recruitmentWidenedAt,
            /** When the last of the people and equipment was pledged; null while anything is missing. */
            Instant resourcesGatheredAt
    ) {
        public static ProjectResponse from(CleanupProject project, long volunteers, long divers, Boolean joined,
                                           List<ParticipantResponse> participants, boolean official) {
            var report = project.getReport();
            boolean finalized = project.getResourcesFinalizedAt() != null;
            return new ProjectResponse(
                    project.getId(),
                    project.getReference(),
                    project.getTitle(),
                    project.getDescription(),
                    project.getStatus(),
                    project.getCompletionPercentage(),
                    project.getLocationName(),
                    project.getProvince(),
                    project.getLatitude(),
                    project.getLongitude(),
                    project.getDebrisRemovedKg(),
                    report == null ? null : report.getId(),
                    report == null ? null : report.getSeverity(),
                    report == null ? List.<ReportDtos.EvidenceResponse>of()
                            : report.getPhotos().stream()
                                    .map(photo -> new ReportDtos.EvidenceResponse(photo.getUrl(), photo.getContentType()))
                                    .toList(),
                    UserDtos.UserSummary.from(project.getOwner()),
                    volunteers,
                    divers,
                    project.getUpdates().stream()
                            .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                            .map(ProjectUpdateResponse::from)
                            .toList(),
                    project.getStartedAt(),
                    project.getCompletedAt(),
                    project.getCreatedAt(),
                    joined,
                    participants,
                    finalized || official ? ResourcesResponse.from(project) : null,
                    official && report != null && report.getAuthorityComment() != null
                            ? new ApprovalNote(UserDtos.UserSummary.from(report.getAuthorityOfficer()),
                                    report.getAuthorityComment(), report.getDecidedAt())
                            : null,
                    project.getRecruitmentRadiusKm(),
                    project.getRecruitmentWidenedAt(),
                    project.getResourcesGatheredAt());
        }
    }

    public record ApprovalNote(UserDtos.UserSummary officer, String comment, Instant decidedAt) {
    }

    public record EquipmentLine(@NotBlank @Size(max = 100) String name, @Min(1) @Max(1000) int quantity) {
    }

    /** One line of equipment a cleanup needs, and how much of it people have promised to bring. */
    public record EquipmentNeed(String name, int quantity, int securedQuantity) {
        public boolean secured() {
            return securedQuantity >= quantity;
        }
    }

    public record ResourcesResponse(
            Integer volunteersNeeded,
            Integer diversNeeded,
            List<EquipmentNeed> equipment,
            boolean finalized,
            Instant finalizedAt,
            UserDtos.UserSummary finalizedBy
    ) {
        static ResourcesResponse from(CleanupProject project) {
            return new ResourcesResponse(
                    project.getVolunteersNeeded(),
                    project.getDiversNeeded(),
                    project.getEquipment().stream()
                            .map(item -> new EquipmentNeed(item.getName(), item.getQuantity(), item.getSecuredQuantity()))
                            .toList(),
                    project.getResourcesFinalizedAt() != null,
                    project.getResourcesFinalizedAt(),
                    UserDtos.UserSummary.from(project.getResourcesFinalizedBy()));
        }
    }

    /** An administrator's resource plan; {@code publish} finalizes and publishes it on the project. */
    public record ResourcesRequest(
            @Min(0) @Max(1000) Integer volunteersNeeded,
            @Min(0) @Max(1000) Integer diversNeeded,
            @Size(max = 30) List<@Valid EquipmentLine> equipment,
            boolean publish
    ) {
    }

    public record ProjectUpdateRequest(
            @NotNull UpdateStage stage,
            @NotBlank @Size(max = 1000) String note,
            String imageUrl,
            @Min(0) @Max(100) Integer completionPercentage,
            Double debrisRemovedKg
    ) {
    }

    public record ProjectUpdateResponse(
            Long id,
            UpdateStage stage,
            String note,
            String imageUrl,
            Integer completionPercentage,
            UserDtos.UserSummary author,
            Instant createdAt
    ) {
        public static ProjectUpdateResponse from(ProjectUpdate update) {
            return new ProjectUpdateResponse(
                    update.getId(),
                    update.getStage(),
                    update.getNote(),
                    update.getImageUrl(),
                    update.getCompletionPercentage(),
                    UserDtos.UserSummary.from(update.getAuthor()),
                    update.getCreatedAt());
        }
    }

    public record JoinProjectRequest(ParticipantRole participantRole) {
    }

    public record ParticipantResponse(
            Long id,
            UserDtos.UserSummary user,
            ParticipantRole role,
            /** The organiser's 1-5 rating, given once the cleanup is complete. */
            Integer mark
    ) {
        public static ParticipantResponse from(ProjectParticipant participant) {
            return new ParticipantResponse(
                    participant.getId(),
                    UserDtos.UserSummary.from(participant.getUser()),
                    participant.getParticipantRole(),
                    participant.getContributionMark());
        }
    }

    public record MarkRequest(@NotNull @Min(1) @Max(5) Integer mark) {
    }
}
