package lk.tideline.cleanup.service;

import lk.tideline.cleanup.dto.UserDtos.AccountReviewRequest;
import lk.tideline.cleanup.dto.UserDtos.AccountReviewResponse;
import lk.tideline.cleanup.dto.UserDtos.AdminUserResponse;
import lk.tideline.cleanup.dto.UserDtos.DocumentDownload;
import lk.tideline.cleanup.dto.UserDtos.OwnedProject;
import lk.tideline.cleanup.dto.UserDtos.PublicProfileResponse;
import lk.tideline.cleanup.dto.UserDtos.SuspensionRequest;
import lk.tideline.cleanup.dto.UserDtos.UpdateDiverProfileRequest;
import lk.tideline.cleanup.dto.UserDtos.UpdateProfileRequest;
import lk.tideline.cleanup.dto.UserDtos.UserResponse;
import lk.tideline.cleanup.model.AccountDocument;
import lk.tideline.cleanup.model.AccountStatus;
import lk.tideline.cleanup.model.AlertType;
import lk.tideline.cleanup.model.DiverProfile;
import lk.tideline.cleanup.model.Role;
import lk.tideline.cleanup.model.User;
import lk.tideline.cleanup.repository.AccountDocumentRepository;
import lk.tideline.cleanup.repository.CleanupProjectRepository;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.ProjectParticipantRepository;
import lk.tideline.cleanup.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    /** Roles an existing administrator must approve before the account can sign in. */
    private static final List<Role> VERIFIED_ROLES = List.of(Role.DIVER, Role.ORGANIZATION, Role.AUTHORITY, Role.ADMIN);

    private final UserRepository userRepository;
    private final ProjectParticipantRepository participantRepository;
    private final CleanupProjectRepository projectRepository;
    private final AccountDocumentRepository documentRepository;
    private final DocumentStorageService storage;
    private final AlertService alertService;
    private final PollutionReportRepository reportRepository;

    public UserService(UserRepository userRepository,
                       ProjectParticipantRepository participantRepository,
                       CleanupProjectRepository projectRepository,
                       AccountDocumentRepository documentRepository,
                       DocumentStorageService storage,
                       AlertService alertService,
                       PollutionReportRepository reportRepository) {
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
        this.projectRepository = projectRepository;
        this.documentRepository = documentRepository;
        this.storage = storage;
        this.alertService = alertService;
        this.reportRepository = reportRepository;
    }

    /** Re-loads inside a transaction so the lazy diver profile can be mapped. */
    @Transactional(readOnly = true)
    public UserResponse view(Long id) {
        return toResponse(get(id));
    }

    /**
     * Someone's public profile. Accounts still waiting for (or refused) verification, and suspended
     * accounts, aren't shown to other members.
     */
    @Transactional(readOnly = true)
    public PublicProfileResponse publicProfile(Long id, User viewer) {
        User user = get(id);
        boolean self = viewer != null && viewer.getId().equals(user.getId());
        boolean admin = viewer != null && viewer.getRole() == Role.ADMIN;
        if (!self && !admin && (user.isSuspended() || user.getAccountStatus() != AccountStatus.APPROVED)) {
            throw new NotFoundException("User " + id + " was not found.");
        }
        Double average = participantRepository.averageMark(user.getId());
        long marked = participantRepository.countByUserIdAndContributionMarkIsNotNull(user.getId());
        List<OwnedProject> owned = projectRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(OwnedProject::from)
                .toList();
        return PublicProfileResponse.from(user,
                average == null ? null : Math.round(average * 10) / 10.0,
                (int) marked,
                reportRepository.countByReporterId(user.getId()),
                owned);
    }

    @Transactional
    public UserResponse updateAvatar(Long userId, MultipartFile photo) {
        User user = get(userId);
        DocumentStorageService.CheckedFile checked = storage.checkAvatar(photo);
        String previous = user.getAvatarStoredName();
        user.setAvatarStoredName(storage.saveAvatar(checked));
        userRepository.save(user);
        storage.deleteAvatar(previous);
        return toResponse(user);
    }

    @Transactional
    public UserResponse removeAvatar(Long userId) {
        User user = get(userId);
        String previous = user.getAvatarStoredName();
        user.setAvatarStoredName(null);
        userRepository.save(user);
        storage.deleteAvatar(previous);
        return toResponse(user);
    }

    private User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " was not found."));
    }

    private UserResponse toResponse(User user) {
        Double average = participantRepository.averageMark(user.getId());
        long marked = participantRepository.countByUserIdAndContributionMarkIsNotNull(user.getId());
        List<OwnedProject> owned = projectRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(OwnedProject::from)
                .toList();
        return UserResponse.from(user,
                average == null ? null : Math.round(average * 10) / 10.0,
                (int) marked,
                owned);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = get(userId);

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.province() != null) {
            user.setProvince(request.province());
        }
        if (request.city() != null) {
            user.setCity(request.city());
        }
        if (request.latitude() != null) {
            user.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            user.setLongitude(request.longitude());
        }
        if (request.availableForAlerts() != null) {
            user.setAvailableForAlerts(request.availableForAlerts());
        }
        if (request.organizationName() != null) {
            user.setOrganizationName(request.organizationName());
        }
        if (request.organizationType() != null) {
            user.setOrganizationType(request.organizationType());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateDiverProfile(Long userId, UpdateDiverProfileRequest request) {
        User user = get(userId);

        if (user.getRole() != Role.DIVER) {
            throw new IllegalStateException("Only a volunteer diver has a diving profile.");
        }

        DiverProfile profile = user.getDiverProfile();
        if (profile == null) {
            profile = new DiverProfile();
            profile.setUser(user);
            user.setDiverProfile(profile);
        }

        if (request.certificationLevel() != null) {
            profile.setCertificationLevel(request.certificationLevel());
        }
        if (request.experienceYears() != null) {
            profile.setExperienceYears(request.experienceYears());
        }
        if (request.equipment() != null) {
            profile.setEquipment(request.equipment());
        }
        if (request.preferredRegions() != null) {
            profile.setPreferredRegions(new ArrayList<>(request.preferredRegions()));
        }

        return toResponse(userRepository.save(user));
    }

    /** Module 4 — account list for administrators, optionally filtered by name or email. */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listForAdmin(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase();
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(user -> needle.isEmpty()
                        || user.getFullName().toLowerCase().contains(needle)
                        || user.getEmail().toLowerCase().contains(needle))
                .map(AdminUserResponse::from)
                .toList();
    }

    /** Module 4 — suspend or reinstate an abusive account. */
    @Transactional
    public AdminUserResponse setSuspension(Long userId, SuspensionRequest request) {
        User user = get(userId);

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Administrator accounts cannot be suspended here.");
        }
        if (request.suspended() && (request.reason() == null || request.reason().isBlank())) {
            throw new IllegalArgumentException("Give a reason for the suspension so other administrators can see why.");
        }

        user.setSuspended(request.suspended());
        user.setSuspensionReason(request.suspended() ? request.reason().trim() : null);
        return AdminUserResponse.from(userRepository.save(user));
    }

    /** Divers and organisations waiting for, or already through, administrator verification. */
    @Transactional(readOnly = true)
    public List<AccountReviewResponse> verifications(AccountStatus status) {
        return userRepository.findByAccountStatusAndRoleInOrderByCreatedAtAsc(status, VERIFIED_ROLES).stream()
                .map(AccountReviewResponse::from)
                .toList();
    }

    @Transactional
    public AccountReviewResponse reviewAccount(Long userId, AccountReviewRequest request) {
        User user = get(userId);

        if (!VERIFIED_ROLES.contains(user.getRole())) {
            throw new IllegalStateException("Community members don't need verification.");
        }
        if (user.getAccountStatus() == AccountStatus.APPROVED) {
            throw new IllegalStateException("This account is already verified.");
        }

        if (request.approved()) {
            user.setAccountStatus(AccountStatus.APPROVED);
            user.setAccountReviewNote(null);
            alertService.send(user, AlertType.ACCOUNT_REVIEW,
                    "Your account has been verified",
                    switch (user.getRole()) {
                        case DIVER -> "An administrator checked your certificates. You can now join cleanups and apply for diving work.";
                        case ADMIN -> "An administrator confirmed your appointment. You now have access to the administration console.";
                        case AUTHORITY -> "An administrator confirmed your appointment. You can now review reports in the government officer console.";
                        default -> "An administrator checked your organisation. You can now post opportunities for divers.";
                    },
                    null, null, null);
        } else {
            if (request.reason() == null || request.reason().isBlank()) {
                throw new IllegalArgumentException("Give a reason. The applicant sees it when they try to sign in.");
            }
            user.setAccountStatus(AccountStatus.REJECTED);
            user.setAccountReviewNote(request.reason().trim());
            // Kept in their alerts too, so the decision is still on record if they are approved later.
            alertService.send(user, AlertType.ACCOUNT_REVIEW,
                    "Your registration wasn't approved",
                    "An administrator reviewed your " + (switch (user.getRole()) {
                        case DIVER -> "certificates";
                        case ADMIN, AUTHORITY -> "appointment";
                        default -> "organisation";
                    })
                            + " and couldn't approve it: " + request.reason().trim(),
                    null, null, null);
        }

        return AccountReviewResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public DocumentDownload document(Long documentId) {
        AccountDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("That document was not found."));
        return new DocumentDownload(document.getOriginalName(), document.getContentType(), storage.read(document));
    }
}
