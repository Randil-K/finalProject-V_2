package lk.tideline.cleanup.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lk.tideline.cleanup.model.AccountDocument;
import lk.tideline.cleanup.model.AccountStatus;
import lk.tideline.cleanup.model.CertificationLevel;
import lk.tideline.cleanup.model.DocumentKind;
import lk.tideline.cleanup.model.CleanupProject;
import lk.tideline.cleanup.model.DiverProfile;
import lk.tideline.cleanup.model.OrganizationType;
import lk.tideline.cleanup.model.ProjectStatus;
import lk.tideline.cleanup.model.Role;
import lk.tideline.cleanup.model.User;

import java.time.Instant;
import java.util.List;

public final class UserDtos {

    private UserDtos() {
    }

    /** Where a member's profile picture is served, or null when they haven't added one. */
    public static String avatarUrlOf(User user) {
        return user == null || user.getAvatarStoredName() == null ? null : "/api/users/avatars/" + user.getAvatarStoredName();
    }

    public record UserResponse(
            Long id,
            String fullName,
            String avatarUrl,
            String email,
            String phone,
            Role role,
            AccountStatus accountStatus,
            String province,
            String city,
            Double latitude,
            Double longitude,
            boolean availableForAlerts,
            String organizationName,
            OrganizationType organizationType,
            String websiteUrl,
            DiverProfileResponse diverProfile,
            /** Average organiser rating (1-5) across completed cleanups; null until someone rates them. */
            Double averageMark,
            Integer markedCleanups,
            /** Projects created from this user's reports — the "project owner" badge. */
            List<OwnedProject> ownedProjects
    ) {
        public static UserResponse from(User user, Double averageMark, Integer markedCleanups, List<OwnedProject> ownedProjects) {
            return new UserResponse(
                    user.getId(),
                    user.getFullName(),
                    avatarUrlOf(user),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getAccountStatus(),
                    user.getProvince(),
                    user.getCity(),
                    user.getLatitude(),
                    user.getLongitude(),
                    user.isAvailableForAlerts(),
                    user.getOrganizationName(),
                    user.getOrganizationType(),
                    user.getWebsiteUrl(),
                    DiverProfileResponse.from(user.getDiverProfile()),
                    averageMark,
                    markedCleanups,
                    ownedProjects);
        }
    }

    /**
     * What other signed-in people see on someone's profile. No email, phone or saved location.
     */
    public record PublicProfileResponse(
            Long id,
            String fullName,
            String avatarUrl,
            Role role,
            boolean verified,
            String province,
            String city,
            Instant memberSince,
            String organizationName,
            OrganizationType organizationType,
            String websiteUrl,
            CertificationLevel certificationLevel,
            Integer experienceYears,
            List<String> preferredRegions,
            Integer completedProjects,
            Double averageMark,
            Integer markedCleanups,
            long reportsSubmitted,
            List<OwnedProject> ownedProjects
    ) {
        public static PublicProfileResponse from(User user, Double averageMark, Integer markedCleanups,
                                                 long reportsSubmitted, List<OwnedProject> ownedProjects) {
            DiverProfile diver = user.getDiverProfile();
            return new PublicProfileResponse(
                    user.getId(),
                    user.getFullName(),
                    avatarUrlOf(user),
                    user.getRole(),
                    user.getAccountStatus() == AccountStatus.APPROVED,
                    user.getProvince(),
                    user.getCity(),
                    user.getCreatedAt(),
                    user.getOrganizationName(),
                    user.getOrganizationType(),
                    user.getWebsiteUrl(),
                    diver == null ? null : diver.getCertificationLevel(),
                    diver == null ? null : diver.getExperienceYears(),
                    diver == null ? null : List.copyOf(diver.getPreferredRegions()),
                    diver == null ? null : diver.getCompletedProjects(),
                    averageMark,
                    markedCleanups,
                    reportsSubmitted,
                    ownedProjects);
        }
    }

    public record OwnedProject(Long id, String reference, String title, ProjectStatus status) {
        public static OwnedProject from(CleanupProject project) {
            return new OwnedProject(project.getId(), project.getReference(), project.getTitle(), project.getStatus());
        }
    }

    /** Compact author reference embedded in reports, comments and projects. */
    public record UserSummary(Long id, String fullName, Role role, String avatarUrl) {
        public static UserSummary from(User user) {
            if (user == null) {
                return null;
            }
            return new UserSummary(user.getId(), user.getFullName(), user.getRole(), avatarUrlOf(user));
        }
    }

    public record DiverProfileResponse(
            CertificationLevel certificationLevel,
            Integer experienceYears,
            String equipment,
            List<String> preferredRegions,
            int completedProjects
    ) {
        public static DiverProfileResponse from(DiverProfile profile) {
            if (profile == null) {
                return null;
            }
            return new DiverProfileResponse(
                    profile.getCertificationLevel(),
                    profile.getExperienceYears(),
                    profile.getEquipment(),
                    profile.getPreferredRegions(),
                    profile.getCompletedProjects());
        }
    }

    public record UpdateProfileRequest(
            String fullName,
            String phone,
            String province,
            String city,
            Double latitude,
            Double longitude,
            Boolean availableForAlerts,
            String organizationName,
            OrganizationType organizationType
    ) {
    }

    public record UpdateDiverProfileRequest(
            CertificationLevel certificationLevel,
            Integer experienceYears,
            String equipment,
            List<String> preferredRegions
    ) {
    }

    /** Admin view of an account (module 4). */
    public record AdminUserResponse(
            Long id,
            String fullName,
            String avatarUrl,
            String email,
            Role role,
            AccountStatus accountStatus,
            String province,
            String city,
            boolean suspended,
            String suspensionReason,
            Instant createdAt
    ) {
        public static AdminUserResponse from(User user) {
            return new AdminUserResponse(
                    user.getId(),
                    user.getFullName(),
                    avatarUrlOf(user),
                    user.getEmail(),
                    user.getRole(),
                    user.getAccountStatus(),
                    user.getProvince(),
                    user.getCity(),
                    user.isSuspended(),
                    user.getSuspensionReason(),
                    user.getCreatedAt());
        }
    }

    public record SuspensionRequest(
            @NotNull Boolean suspended,
            @Size(max = 500) String reason
    ) {
    }

    /** What an administrator checks before verifying a diver or organisation. */
    public record AccountReviewResponse(
            Long id,
            String fullName,
            String email,
            String phone,
            Role role,
            AccountStatus accountStatus,
            String reviewNote,
            String province,
            String city,
            CertificationLevel certificationLevel,
            Integer experienceYears,
            String organizationName,
            OrganizationType organizationType,
            String websiteUrl,
            List<DocumentResponse> documents,
            Instant createdAt
    ) {
        public static AccountReviewResponse from(User user) {
            DiverProfile profile = user.getDiverProfile();
            return new AccountReviewResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getAccountStatus(),
                    user.getAccountReviewNote(),
                    user.getProvince(),
                    user.getCity(),
                    profile == null ? null : profile.getCertificationLevel(),
                    profile == null ? null : profile.getExperienceYears(),
                    user.getOrganizationName(),
                    user.getOrganizationType(),
                    user.getWebsiteUrl(),
                    user.getDocuments().stream().map(DocumentResponse::from).toList(),
                    user.getCreatedAt());
        }
    }

    public record DocumentResponse(Long id, String name, DocumentKind kind, String contentType, long sizeBytes, Instant uploadedAt) {
        public static DocumentResponse from(AccountDocument document) {
            return new DocumentResponse(
                    document.getId(),
                    document.getOriginalName(),
                    document.getKind() == null ? DocumentKind.CERTIFICATE : document.getKind(),
                    document.getContentType(),
                    document.getSizeBytes(),
                    document.getUploadedAt());
        }
    }

    public record AccountReviewRequest(
            @NotNull Boolean approved,
            @Size(max = 500) String reason
    ) {
    }

    public record DocumentDownload(String name, String contentType, byte[] bytes) {
    }
}
