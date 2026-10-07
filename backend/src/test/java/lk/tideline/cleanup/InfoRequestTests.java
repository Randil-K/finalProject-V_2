package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.InfoRequestDtos.InfoRequestResponse;
import lk.tideline.cleanup.dto.ReportDtos.CreateReportRequest;
import lk.tideline.cleanup.dto.ReportDtos.ModerationRequest;
import lk.tideline.cleanup.dto.ReportDtos.ReportResponse;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.AlertRepository;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.InfoRequestService;
import lk.tideline.cleanup.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Modules 3 and 4 — the 5-confirmation rule, and reviewers asking the reporter for more information. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class InfoRequestTests {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Autowired
    private ReportService reportService;

    @Autowired
    private InfoRequestService infoRequests;

    @Autowired
    private UserRepository users;

    @Autowired
    private AlertRepository alerts;

    @Test
    void aReportNeedsFiveConfirmationsBeforeItReachesTheAdministrator() {
        User admin = user(Role.ADMIN);
        User reporter = user(Role.CITIZEN);
        Long id = report(reporter);

        ReportResponse response = null;
        for (int i = 0; i < 4; i++) {
            response = reportService.vote(id, user(Role.CITIZEN), true);
        }
        assertThat(response.trustPercentage()).isEqualTo(100);
        assertThat(response.status()).isEqualTo(ReportStatus.VERIFYING);

        response = reportService.vote(id, user(Role.CITIZEN), true);
        assertThat(response.status()).isEqualTo(ReportStatus.VERIFIED);
        assertThat(titles(admin)).contains("Report ready for your review");
    }

    @Test
    void onlyVerifiedReportsReachTheReviewQueue() {
        Long unverified = report(user(Role.CITIZEN));
        reportService.vote(unverified, user(Role.CITIZEN), true);
        Long verified = verifiedReport(user(Role.CITIZEN));
        PageRequest page = PageRequest.of(0, 500);

        assertThat(reportService.search(null, null, null, true, page).getContent())
                .extracting(ReportResponse::id)
                .contains(verified)
                .doesNotContain(unverified);
        assertThat(reportService.search(ReportStatus.VERIFYING, null, null, true, page).getContent()).isEmpty();
        assertThat(reportService.search(null, null, null, false, page).getContent())
                .extracting(ReportResponse::id)
                .contains(unverified, verified);
    }

    @Test
    void fiveConfirmationsStillNeedSeventyFivePercentTrust() {
        Long id = report(user(Role.CITIZEN));
        for (int i = 0; i < 2; i++) {
            reportService.vote(id, user(Role.CITIZEN), false);
        }
        ReportResponse response = null;
        for (int i = 0; i < 5; i++) {
            response = reportService.vote(id, user(Role.CITIZEN), true);
        }
        // 5 of 7 is 71%.
        assertThat(response.status()).isEqualTo(ReportStatus.VERIFYING);
    }

    @Test
    void votingTheSameWayAgainRemovesTheVote() {
        Long id = report(user(Role.CITIZEN));
        User voter = user(Role.CITIZEN);

        ReportResponse confirmed = reportService.vote(id, voter, true);
        assertThat(confirmed.confirmVotes()).isEqualTo(1);
        assertThat(confirmed.myVote()).isTrue();

        ReportResponse switched = reportService.vote(id, voter, false);
        assertThat(switched.confirmVotes()).isZero();
        assertThat(switched.disputeVotes()).isEqualTo(1);
        assertThat(switched.myVote()).isFalse();

        ReportResponse removed = reportService.vote(id, voter, false);
        assertThat(removed.disputeVotes()).isZero();
        assertThat(removed.myVote()).isNull();
        assertThat(removed.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(reportService.view(id, voter).myVote()).isNull();
    }

    @Test
    void withdrawingAConfirmationTakesAnUnreviewedReportBackBelowTheThreshold() {
        Long id = report(user(Role.CITIZEN));
        User last = null;
        for (int i = 0; i < 5; i++) {
            last = user(Role.CITIZEN);
            reportService.vote(id, last, true);
        }
        assertThat(reportService.view(id).status()).isEqualTo(ReportStatus.VERIFIED);

        ReportResponse withdrawn = reportService.vote(id, last, true);
        assertThat(withdrawn.confirmVotes()).isEqualTo(4);
        assertThat(withdrawn.status()).isEqualTo(ReportStatus.VERIFYING);
    }

    @Test
    void reportersCannotVoteOnTheirOwnReport() {
        User reporter = user(Role.CITIZEN);
        Long id = report(reporter);
        assertThatThrownBy(() -> reportService.vote(id, reporter, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own report");
    }

    @Test
    void administratorsApproveOnlyAfterVerificationButCanRejectAnytime() {
        User admin = user(Role.ADMIN);
        Long id = report(user(Role.CITIZEN));

        assertThatThrownBy(() -> reportService.moderate(id, new ModerationRequest(ReviewDecision.APPROVED, null), admin))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> reportService.moderate(id,
                new ModerationRequest(ReviewDecision.MORE_INFO_REQUESTED, "Where exactly?"), admin))
                .isInstanceOf(IllegalStateException.class);

        ReportResponse rejected = reportService.moderate(id, new ModerationRequest(ReviewDecision.REJECTED, "Duplicate."), admin);
        assertThat(rejected.status()).isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    void theReporterAnswersWithPhotosThatOnlyReviewersAndTheReporterCanSee() throws Exception {
        User admin = user(Role.ADMIN);
        User reporter = user(Role.CITIZEN);
        User neighbour = user(Role.CITIZEN);
        Long id = verifiedReport(reporter);

        reportService.moderate(id, new ModerationRequest(ReviewDecision.MORE_INFO_REQUESTED, "Send a closer photo."), admin);
        assertThatThrownBy(() -> reportService.moderate(id,
                new ModerationRequest(ReviewDecision.MORE_INFO_REQUESTED, "And another."), admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hasn't answered");

        InfoRequestResponse open = infoRequests.list(id, reporter).get(0);
        assertThat(open.status()).isEqualTo(InfoRequestStatus.OPEN);
        assertThat(open.message()).isEqualTo("Send a closer photo.");

        assertThatThrownBy(() -> infoRequests.respond(id, open.id(), neighbour, "Not mine.", List.of()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> infoRequests.list(id, neighbour)).isInstanceOf(AccessDeniedException.class);

        InfoRequestResponse answered = infoRequests.respond(id, open.id(), reporter, "Taken from the jetty.",
                List.of(new MockMultipartFile("photos", "closeup.png", "image/png", PNG)));

        assertThat(answered.status()).isEqualTo(InfoRequestStatus.ANSWERED);
        assertThat(answered.responseText()).isEqualTo("Taken from the jetty.");
        assertThat(answered.attachments()).hasSize(1);
        assertThat(titles(admin)).contains("More information received on " + answered.reportReference());
        assertThat(alerts.findByRecipientOrderByCreatedAtDesc(reporter))
                .filteredOn(alert -> alert.getType() == AlertType.INFO_REQUESTED)
                .allMatch(Alert::isReadFlag);

        String storedName = answered.attachments().get(0).url().substring("/api/reports/info-attachments/".length());
        assertThat(Files.readAllBytes(infoRequests.attachment(storedName, admin).path())).isEqualTo(PNG);
        assertThat(infoRequests.attachment(storedName, reporter)).isNotNull();
        assertThatThrownBy(() -> infoRequests.attachment(storedName, neighbour)).isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> infoRequests.respond(id, open.id(), reporter, "Again.", List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void videosCannotBeAttachedToAnAnswer() {
        User reporter = user(Role.CITIZEN);
        Long id = verifiedReport(reporter);
        reportService.moderate(id, new ModerationRequest(ReviewDecision.MORE_INFO_REQUESTED, "More detail."), user(Role.ADMIN));
        Long requestId = infoRequests.list(id, reporter).get(0).id();
        byte[] mp4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};

        assertThatThrownBy(() -> infoRequests.respond(id, requestId, reporter, "Video.",
                List.of(new MockMultipartFile("photos", "clip.mp4", "video/mp4", mp4))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a photo");
    }

    private Long verifiedReport(User reporter) {
        Long id = report(reporter);
        for (int i = 0; i < 5; i++) {
            reportService.vote(id, user(Role.CITIZEN), true);
        }
        return id;
    }

    private Long report(User reporter) {
        return reportService.create(new CreateReportRequest("Plastic on the shore", "Bottles.", Severity.LOW,
                "Galle", "Southern Province", 6.03, 80.21, null), reporter).id();
    }

    private List<String> titles(User user) {
        return alerts.findByRecipientOrderByCreatedAtDesc(user).stream().map(Alert::getTitle).toList();
    }

    private User user(Role role) {
        User user = new User();
        user.setFullName(role.name() + " " + UUID.randomUUID().toString().substring(0, 6));
        user.setEmail(UUID.randomUUID() + "@test.lk");
        user.setPasswordHash("x");
        user.setRole(role);
        return users.save(user);
    }
}
