package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.ReportDtos.ReportResponse;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The feed: everyone's own province, filtered by severity and date, with rejections set apart. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class FeedTests {

    private static final PageRequest PAGE = PageRequest.of(0, 500);

    @Autowired
    private UserRepository users;

    @Autowired
    private PollutionReportRepository reports;

    @Autowired
    private ReportService reportService;

    @Test
    void theFeedShowsTheViewerTheirOwnProvinceOnly() {
        User viewer = user("Southern Province");
        PollutionReport near = report("Southern Province", Severity.LOW, ReportStatus.PENDING);
        PollutionReport far = report("Northern Province", Severity.LOW, ReportStatus.PENDING);

        List<ReportResponse> feed = reportService.search(null, null, null, false, false, viewer, PAGE).getContent();

        assertThat(feed).extracting(ReportResponse::id).contains(near.getId()).doesNotContain(far.getId());
        assertThat(feed).extracting(ReportResponse::province).containsOnly("Southern Province");
    }

    @Test
    void aVisitorWhoHasToldUsNothingSeesTheWholeCoast() {
        PollutionReport south = report("Southern Province", Severity.LOW, ReportStatus.PENDING);
        PollutionReport north = report("Northern Province", Severity.LOW, ReportStatus.PENDING);

        assertThat(reportService.search(null, null, null, false, false, null, PAGE).getContent())
                .extracting(ReportResponse::id)
                .contains(south.getId(), north.getId());
    }

    @Test
    void rejectedReportsAreOutOfTheWayUntilTheyAreAskedFor() {
        User viewer = user("Sabaragamuwa Province");
        PollutionReport open = report("Sabaragamuwa Province", Severity.LOW, ReportStatus.PENDING);
        PollutionReport turnedDown = report("Sabaragamuwa Province", Severity.LOW, ReportStatus.REJECTED);

        assertThat(reportService.search(null, null, null, false, false, viewer, PAGE).getContent())
                .extracting(ReportResponse::id)
                .contains(open.getId())
                .doesNotContain(turnedDown.getId());

        assertThat(reportService.search(ReportStatus.REJECTED, null, null, false, false, viewer, PAGE).getContent())
                .extracting(ReportResponse::id)
                .containsExactly(turnedDown.getId());
    }

    @Test
    void theFeedNarrowsBySeverityAndTurnsAroundByDate() {
        User viewer = user("Uva Province");
        PollutionReport first = reportedDaysAgo("Uva Province", Severity.CRITICAL, 9);
        PollutionReport mild = reportedDaysAgo("Uva Province", Severity.LOW, 5);
        PollutionReport last = reportedDaysAgo("Uva Province", Severity.CRITICAL, 2);

        assertThat(reportService.search(null, Severity.CRITICAL, null, false, false, viewer, PAGE).getContent())
                .extracting(ReportResponse::id)
                .containsExactly(last.getId(), first.getId())
                .doesNotContain(mild.getId());

        assertThat(reportService.search(null, Severity.CRITICAL, null, false, true, viewer, PAGE).getContent())
                .extracting(ReportResponse::id)
                .containsExactly(first.getId(), last.getId());
    }

    private User user(String province) {
        User user = new User();
        user.setFullName("Feed tester");
        user.setEmail(UUID.randomUUID() + "@test.lk");
        user.setPasswordHash("not-used");
        user.setRole(Role.CITIZEN);
        user.setProvince(province);
        return users.save(user);
    }

    /** Spaced out in time so the newest-first and oldest-first orders cannot tie. */
    private PollutionReport reportedDaysAgo(String province, Severity severity, int days) {
        PollutionReport report = report(province, severity, ReportStatus.PENDING);
        report.setCreatedAt(Instant.now().minus(Duration.ofDays(days)));
        return reports.save(report);
    }

    private PollutionReport report(String province, Severity severity, ReportStatus status) {
        PollutionReport report = new PollutionReport();
        report.setReference("F-" + UUID.randomUUID());
        report.setTitle("Feed test debris");
        report.setDescription("Feed test report.");
        report.setSeverity(severity);
        report.setStatus(status);
        report.setLocationName("Test bay");
        report.setLatitude(6.0535);
        report.setLongitude(80.2210);
        report.setProvince(province);
        report.setReporter(user(province));
        return reports.save(report);
    }
}
