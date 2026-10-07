package lk.tideline.cleanup.service;

import lk.tideline.cleanup.dto.AuthDtos.AuthResponse;
import lk.tideline.cleanup.dto.AuthDtos.LoginRequest;
import lk.tideline.cleanup.dto.AuthDtos.RegisterRequest;
import lk.tideline.cleanup.dto.UserDtos.UserResponse;
import lk.tideline.cleanup.model.AccountStatus;
import lk.tideline.cleanup.model.AlertType;
import lk.tideline.cleanup.model.DiverProfile;
import lk.tideline.cleanup.model.DocumentKind;
import lk.tideline.cleanup.model.Role;
import lk.tideline.cleanup.model.User;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.security.JwtService;
import lk.tideline.cleanup.service.DocumentStorageService.CheckedFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final DocumentStorageService storage;
    private final AlertService alertService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserService userService,
                       DocumentStorageService storage,
                       AlertService alertService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.storage = storage;
        this.alertService = alertService;
    }

    /**
     * Community members can sign in straight away. Volunteer divers (with certificates), organisations
     * (with a website), and administrators and government officers (with their department and proof of
     * appointment) wait for an existing administrator to approve them, so they get no token.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, List<MultipartFile> certificates) {
        return register(request, certificates, List.of());
    }

    public AuthResponse register(RegisterRequest request, List<MultipartFile> certificates, List<MultipartFile> licences) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalStateException("An account already uses that email address.");
        }

        Role role = request.role() == null ? Role.CITIZEN : request.role();
        boolean official = role == Role.ADMIN || role == Role.AUTHORITY;

        List<CheckedFile> files = storage.check(certificates);
        List<CheckedFile> licenceFiles = storage.check(licences);
        if (role == Role.DIVER && files.isEmpty()) {
            throw new IllegalArgumentException("Attach at least one diving certificate so an administrator can verify you.");
        }
        if (official && files.isEmpty()) {
            throw new IllegalArgumentException("Attach proof of your appointment, such as a staff ID or appointment letter.");
        }
        if (role != Role.DIVER && !official && !files.isEmpty()) {
            throw new IllegalArgumentException("Only volunteer divers, administrators and government officers attach documents.");
        }
        if (role != Role.DIVER && !licenceFiles.isEmpty()) {
            throw new IllegalArgumentException("Only volunteer divers attach licences.");
        }
        if (official && (request.organizationName() == null || request.organizationName().isBlank())) {
            throw new IllegalArgumentException("Add the department or agency you work for.");
        }

        String website = null;
        if (role == Role.ORGANIZATION) {
            if (request.organizationName() == null || request.organizationName().isBlank()) {
                throw new IllegalArgumentException("Add your organisation's name.");
            }
            website = normaliseWebsite(request.websiteUrl());
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(role);
        user.setProvince(request.province());
        user.setCity(request.city());
        user.setLatitude(request.latitude());
        user.setLongitude(request.longitude());
        user.setOrganizationName(request.organizationName());
        user.setOrganizationType(role == Role.ORGANIZATION ? request.organizationType() : null);
        user.setWebsiteUrl(website);
        user.setAccountStatus(role == Role.CITIZEN ? AccountStatus.APPROVED : AccountStatus.PENDING_REVIEW);

        if (role == Role.DIVER) {
            DiverProfile profile = new DiverProfile();
            profile.setUser(user);
            profile.setCertificationLevel(request.certificationLevel());
            user.setDiverProfile(profile);
        }

        User saved = userRepository.save(user);
        DocumentKind attached = official ? DocumentKind.APPOINTMENT : DocumentKind.CERTIFICATE;
        for (CheckedFile file : files) {
            saved.getDocuments().add(storage.save(saved, file, attached));
        }
        for (CheckedFile file : licenceFiles) {
            saved.getDocuments().add(storage.save(saved, file, DocumentKind.LICENCE));
        }
        userRepository.saveAndFlush(saved);

        UserResponse view = userService.view(saved.getId());
        if (saved.getAccountStatus() != AccountStatus.APPROVED) {
            notifyAdministrators(saved, files.size() + licenceFiles.size());
            return new AuthResponse(null, 0, view);
        }
        return new AuthResponse(jwtService.issueToken(saved), jwtService.expirySeconds(), view);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Bad credentials");
        }
        // Status is only revealed to someone who knows the password.
        if (user.isSuspended()) {
            throw new DisabledException("Suspended");
        }
        if (user.getAccountStatus() == AccountStatus.PENDING_REVIEW) {
            throw new AccountReviewException("ACCOUNT_PENDING",
                    "Your account is waiting for an administrator to verify it. You can sign in once it's approved.");
        }
        if (user.getAccountStatus() == AccountStatus.REJECTED) {
            String note = user.getAccountReviewNote();
            throw new AccountReviewException("ACCOUNT_REJECTED",
                    "Your account application wasn't approved" + (note == null ? "." : ": " + note));
        }

        return new AuthResponse(jwtService.issueToken(user, request.remember()),
                jwtService.expirySeconds(request.remember()), userService.view(user.getId()));
    }

    private void notifyAdministrators(User applicant, int documentCount) {
        String documents = documentCount + (documentCount == 1 ? " document." : " documents.");
        String title;
        String body;
        switch (applicant.getRole()) {
            case DIVER -> {
                title = "New volunteer diver to verify";
                body = applicant.getFullName() + " registered with " + documentCount
                        + (documentCount == 1 ? " certificate." : " certificates.");
            }
            case ADMIN -> {
                title = "New administrator to verify";
                body = applicant.getFullName() + " (" + applicant.getOrganizationName() + ") asked for administrator access with " + documents;
            }
            case AUTHORITY -> {
                title = "New government officer to verify";
                body = applicant.getFullName() + " (" + applicant.getOrganizationName() + ") registered as a government officer with " + documents;
            }
            default -> {
                title = "New organisation to verify";
                body = applicant.getOrganizationName() + " registered. Check " + applicant.getWebsiteUrl() + " before approving.";
            }
        }
        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            alertService.send(admin, AlertType.ACCOUNT_APPLICATION, title, body, null, null, null);
        }
    }

    static String normaliseWebsite(String raw) {
        String invalid = "That website link isn't valid. It should look like https://example.org";
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Add your organisation's website so an administrator can check it.");
        }
        URI uri;
        try {
            uri = new URI(raw.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(invalid);
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                || host == null || !host.contains(".")) {
            throw new IllegalArgumentException(invalid);
        }
        return uri.toString();
    }
}
