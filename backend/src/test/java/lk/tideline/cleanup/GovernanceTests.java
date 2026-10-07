package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.AuthDtos.AuthResponse;
import lk.tideline.cleanup.dto.AuthDtos.LoginRequest;
import lk.tideline.cleanup.dto.AuthDtos.RegisterRequest;
import lk.tideline.cleanup.dto.ProjectDtos.EquipmentLine;
import lk.tideline.cleanup.dto.ReportDtos.ApprovalResources;
import lk.tideline.cleanup.dto.ReportDtos.AuthorityDecisionRequest;
import lk.tideline.cleanup.dto.ReportDtos.ModerationRequest;
import lk.tideline.cleanup.dto.ReportDtos.ReportResponse;
import lk.tideline.cleanup.dto.UserDtos.AccountReviewRequest;
import lk.tideline.cleanup.dto.UserDtos.DocumentResponse;
import lk.tideline.cleanup.dto.UserDtos.SuspensionRequest;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.AlertRepository;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.AccountReviewException;
import lk.tideline.cleanup.service.AuthService;
import lk.tideline.cleanup.service.ReportService;
import lk.tideline.cleanup.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Account verification, moderation, and the administrator and authority review decisions. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class GovernanceTests {

    @Autowired
    private UserRepository users;

    @Autowired
    private PollutionReportRepository reports;

    @Autowired
    private AlertRepository alerts;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReportService reportService;

    // ---- account verification ----

    @Test
    void aDiverWaitsForVerificationBeforeSigningIn() {
        User admin = user(Role.ADMIN);
        String email = UUID.randomUUID() + "@test.lk";

        AuthResponse registered = authService.register(
                registration(email, Role.DIVER, null, null), List.of(pdf("padi.pdf")));

        assertThat(registered.token()).isNull();
        assertThat(registered.user().accountStatus()).isEqualTo(AccountStatus.PENDING_REVIEW);
        assertThat(titlesFor(admin)).contains("New volunteer diver to verify");
        // A notice about someone else's registration, not a decision on the administrator's own account.
        assertThat(alerts.findByRecipientOrderByCreatedAtDesc(admin))
                .filteredOn(alert -> alert.getTitle().equals("New volunteer diver to verify"))
                .extracting(Alert::getType)
                .containsOnly(AlertType.ACCOUNT_APPLICATION);
        assertThat(userService.verifications(AccountStatus.PENDING_REVIEW))
                .anySatisfy(account -> {
                    assertThat(account.email()).isEqualTo(email);
                    assertThat(account.documents()).hasSize(1);
                });

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "password123", null)))
                .isInstanceOf(AccountReviewException.class)
                .extracting("code").isEqualTo("ACCOUNT_PENDING");

        userService.reviewAccount(registered.user().id(), new AccountReviewRequest(true, null));
        assertThat(authService.login(new LoginRequest(email, "password123", null)).token()).isNotBlank();
        assertThat(titlesFor(users.findByEmailIgnoreCase(email).orElseThrow())).contains("Your account has been verified");
    }

    @Test
    void aRejectedApplicantSeesTheReasonWhenSigningIn() {
        String email = UUID.randomUUID() + "@test.lk";
        AuthResponse registered = authService.register(
                registration(email, Role.DIVER, null, null), List.of(pdf("card.pdf")));

        assertThatThrownBy(() -> userService.reviewAccount(registered.user().id(), new AccountReviewRequest(false, " ")))
                .isInstanceOf(IllegalArgumentException.class);

        userService.reviewAccount(registered.user().id(), new AccountReviewRequest(false, "Certificate has expired."));
        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "password123", null)))
                .isInstanceOf(AccountReviewException.class)
                .hasMessageContaining("Certificate has expired.");
        assertThat(titlesFor(users.findByEmailIgnoreCase(email).orElseThrow())).contains("Your registration wasn't approved");
    }

    @Test
    void governmentOfficersAndAdministratorsWaitForAnExistingAdministrator() {
        User admin = user(Role.ADMIN);
        String officerEmail = UUID.randomUUID() + "@test.lk";
        String adminEmail = UUID.randomUUID() + "@test.lk";

        AuthResponse officer = authService.register(
                registration(officerEmail, Role.AUTHORITY, "Marine Environment Protection Authority", null), List.of(pdf("staff-id.pdf")));
        AuthResponse newAdmin = authService.register(
                registration(adminEmail, Role.ADMIN, "Tideline operations", null), List.of(pdf("appointment.pdf")));

        assertThat(officer.token()).isNull();
        assertThat(newAdmin.token()).isNull();
        assertThat(officer.user().accountStatus()).isEqualTo(AccountStatus.PENDING_REVIEW);
        assertThat(titlesFor(admin)).contains("New government officer to verify", "New administrator to verify");
        assertThat(userService.verifications(AccountStatus.PENDING_REVIEW))
                .extracting(account -> account.email())
                .contains(officerEmail, adminEmail);
        assertThatThrownBy(() -> authService.login(new LoginRequest(officerEmail, "password123", null)))
                .isInstanceOf(AccountReviewException.class);

        userService.reviewAccount(officer.user().id(), new AccountReviewRequest(true, null));
        assertThat(authService.login(new LoginRequest(officerEmail, "password123", null)).user().role()).isEqualTo(Role.AUTHORITY);
    }

    @Test
    void officialsMustGiveTheirDepartmentAndProofOfAppointment() {
        assertThatThrownBy(() -> authService.register(
                registration(UUID.randomUUID() + "@test.lk", Role.AUTHORITY, "MEPA", null), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proof of your appointment");
        assertThatThrownBy(() -> authService.register(
                registration(UUID.randomUUID() + "@test.lk", Role.ADMIN, null, null), List.of(pdf("id.pdf"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("department");
    }

    @Test
    void aDiverMustAttachACertificate() {
        assertThatThrownBy(() -> authService.register(
                registration(UUID.randomUUID() + "@test.lk", Role.DIVER, null, null), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("certificate");
    }

    @Test
    void aDiverCanAttachCertificatesAndLicencesSeparately() {
        String email = UUID.randomUUID() + "@test.lk";

        authService.register(registration(email, Role.DIVER, null, null),
                List.of(pdf("padi.pdf")), List.of(pdf("boat-licence.pdf")));

        assertThat(userService.verifications(AccountStatus.PENDING_REVIEW))
                .filteredOn(account -> account.email().equals(email))
                .singleElement()
                .satisfies(account -> assertThat(account.documents())
                        .extracting(DocumentResponse::kind)
                        .containsExactlyInAnyOrder(DocumentKind.CERTIFICATE, DocumentKind.LICENCE));
    }

    @Test
    void onlyDiversAttachLicences() {
        assertThatThrownBy(() -> authService.register(
                registration(UUID.randomUUID() + "@test.lk", Role.CITIZEN, null, null), List.of(), List.of(pdf("licence.pdf"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("licences");
    }

    @Test
    void aFileIsCheckedByItsContentsNotItsName() {
        MultipartFile disguised = new MockMultipartFile("certificates", "certificate.pdf", "application/pdf",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> authService.register(
                registration(UUID.randomUUID() + "@test.lk", Role.DIVER, null, null), List.of(disguised)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a PDF, JPG or PNG");
    }

    @Test
    void anOrganisationNeedsAValidWebsite() {
        assertThatThrownBy(() -> authService.register(
                registration(UUID.randomUUID() + "@test.lk", Role.ORGANIZATION, "Reef Friends", "javascript:alert(1)"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("website");

        AuthResponse registered = authService.register(
                registration(UUID.randomUUID() + "@test.lk", Role.ORGANIZATION, "Reef Friends", "https://reef-friends.example.org"),
                List.of());
        assertThat(registered.token()).isNull();
        assertThat(registered.user().websiteUrl()).isEqualTo("https://reef-friends.example.org");
    }

    @Test
    void aCommunityMemberCanSignInStraightAway() {
        AuthResponse registered = authService.register(
                registration(UUID.randomUUID() + "@test.lk", Role.CITIZEN, null, null), List.of());
        assertThat(registered.token()).isNotBlank();
        assertThat(registered.user().accountStatus()).isEqualTo(AccountStatus.APPROVED);
    }

    // ---- suspension ----

    @Test
    void aSuspendedMemberCannotSignInUntilReinstated() {
        String email = UUID.randomUUID() + "@test.lk";
        AuthResponse registered = authService.register(registration(email, Role.CITIZEN, null, null), List.of());

        userService.setSuspension(registered.user().id(), new SuspensionRequest(true, "Repeated false reports."));
        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "password123", null)))
                .isInstanceOf(DisabledException.class);

        userService.setSuspension(registered.user().id(), new SuspensionRequest(false, null));
        assertThat(authService.login(new LoginRequest(email, "password123", null)).token()).isNotBlank();
    }

    @Test
    void administratorsCannotBeSuspended() {
        User admin = user(Role.ADMIN);
        assertThatThrownBy(() -> userService.setSuspension(admin.getId(), new SuspensionRequest(true, "Test")))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- report review ----

    @Test
    void adminApprovalSendsTheReportToTheAuthorityAndAlertsOfficers() {
        User officer = user(Role.AUTHORITY);
        User admin = user(Role.ADMIN);
        PollutionReport report = report(user(Role.CITIZEN), ReportStatus.VERIFIED);

        ReportResponse approved = reportService.moderate(report.getId(), new ModerationRequest(ReviewDecision.APPROVED, null), admin);

        assertThat(approved.status()).isEqualTo(ReportStatus.ESCALATED);
        assertThat(approved.adminDecision()).isEqualTo(ReviewDecision.APPROVED);
        assertThat(approved.authorityDecision()).isEqualTo(ReviewDecision.PENDING);
        assertThat(titlesFor(officer)).contains("Report escalated for your review");

        reportService.decideAsAuthority(report.getId(),
                new AuthorityDecisionRequest(ReviewDecision.APPROVED, "Approved with conditions.",
                        new ApprovalResources(12, 2, List.of(new EquipmentLine("Lift bags", 4)))), officer);
        assertThat(titlesFor(admin)).anyMatch(title -> title.startsWith("Government officer approved "));
    }

    @Test
    void anAdminInformationRequestIsACriticalAlertAndStaysOutOfTheDiscussion() {
        User reporter = user(Role.CITIZEN);
        PollutionReport report = report(reporter, ReportStatus.VERIFIED);

        ReportResponse response = reportService.moderate(report.getId(),
                new ModerationRequest(ReviewDecision.MORE_INFO_REQUESTED, "Which end of the beach is this?"), user(Role.ADMIN));

        assertThat(response.adminDecision()).isEqualTo(ReviewDecision.MORE_INFO_REQUESTED);
        assertThat(response.status()).isEqualTo(ReportStatus.VERIFIED);
        assertThat(response.infoRequestStatus()).isEqualTo(InfoRequestStatus.OPEN);
        assertThat(alerts.findByRecipientOrderByCreatedAtDesc(reporter)).anySatisfy(alert -> {
            assertThat(alert.getType()).isEqualTo(AlertType.INFO_REQUESTED);
            assertThat(alert.isCritical()).isTrue();
        });
        assertThat(reportService.comments(report.getId(), null)).isEmpty();
    }

    @Test
    void aClarificationRequestMustSayWhatIsUnclear() {
        PollutionReport report = report(user(Role.CITIZEN), ReportStatus.VERIFIED);
        assertThatThrownBy(() -> reportService.moderate(report.getId(),
                new ModerationRequest(ReviewDecision.MORE_INFO_REQUESTED, null), user(Role.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moderationWaitsWhileTheAuthorityDecides() {
        PollutionReport report = report(user(Role.CITIZEN), ReportStatus.ESCALATED);
        assertThatThrownBy(() -> reportService.moderate(report.getId(),
                new ModerationRequest(ReviewDecision.REJECTED, "No."), user(Role.ADMIN)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theAuthorityCanAskForMoreInformationWithoutDeciding() {
        User reporter = user(Role.CITIZEN);
        PollutionReport report = report(reporter, ReportStatus.ESCALATED);

        ReportResponse response = reportService.decideAsAuthority(report.getId(),
                new AuthorityDecisionRequest(ReviewDecision.MORE_INFO_REQUESTED, "How deep is the sheen?", null), user(Role.AUTHORITY));

        assertThat(response.status()).isEqualTo(ReportStatus.ESCALATED);
        assertThat(response.authorityDecision()).isEqualTo(ReviewDecision.MORE_INFO_REQUESTED);
        assertThat(response.projectId()).isNull();
        assertThat(titlesFor(reporter)).contains("Action needed: more information on " + report.getReference());
    }

    @Test
    void anAuthorityRejectionClosesTheReport() {
        PollutionReport report = report(user(Role.CITIZEN), ReportStatus.ESCALATED);

        ReportResponse response = reportService.decideAsAuthority(report.getId(),
                new AuthorityDecisionRequest(ReviewDecision.REJECTED, "Outside coastal jurisdiction.", null), user(Role.AUTHORITY));

        assertThat(response.status()).isEqualTo(ReportStatus.REJECTED);
        assertThat(response.projectId()).isNull();
    }

    private static RegisterRequest registration(String email, Role role, String organisationName, String website) {
        return new RegisterRequest("Test Person", email, "password123", null, role,
                null, null, null, null, organisationName, null, null, website);
    }

    private static MultipartFile pdf(String name) {
        return new MockMultipartFile("certificates", name, "application/pdf",
                "%PDF-1.4\n% test certificate\n".getBytes(StandardCharsets.US_ASCII));
    }

    private List<String> titlesFor(User user) {
        return alerts.findByRecipientOrderByCreatedAtDesc(user).stream().map(Alert::getTitle).toList();
    }

    private User user(Role role) {
        User user = new User();
        user.setFullName(role.name() + " tester");
        user.setEmail(UUID.randomUUID() + "@test.lk");
        user.setPasswordHash("not-used");
        user.setRole(role);
        return users.save(user);
    }

    private PollutionReport report(User reporter, ReportStatus status) {
        PollutionReport report = new PollutionReport();
        report.setReference("T-" + UUID.randomUUID());
        report.setTitle("Test debris");
        report.setDescription("Test report.");
        report.setSeverity(Severity.MEDIUM);
        report.setStatus(status);
        if (status == ReportStatus.ESCALATED) {
            report.setAdminDecision(ReviewDecision.APPROVED);
            report.setAuthorityDecision(ReviewDecision.PENDING);
        }
        report.setLocationName("Negombo");
        report.setProvince("Western Province");
        report.setLatitude(7.2083);
        report.setLongitude(79.8358);
        report.setReporter(reporter);
        return reports.save(report);
    }
}
