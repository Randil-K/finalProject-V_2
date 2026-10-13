package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.AnalyticsDtos.SummaryResponse;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The dashboard narrowed to one district. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class AnalyticsTests {

    @Autowired
    private AnalyticsService analytics;

    @Autowired
    private PollutionReportRepository reports;

    @Autowired
    private UserRepository users;

    @Test
    void aDistrictSeesItsOwnFiguresAndNobodyElses() {
        SummaryResponse before = analytics.summary("Galle");
        report("Galle", "Southern Province", ReportStatus.VERIFIED);
        report("Galle", "Southern Province", ReportStatus.VERIFIED);
        report("Jaffna", "Northern Province", ReportStatus.VERIFIED);

        SummaryResponse galle = analytics.summary("Galle");

        assertThat(galle.district()).isEqualTo("Galle");
        assertThat(galle.province()).as("accounts record a province, so the people counts follow it")
                .isEqualTo("Southern Province");
        assertThat(galle.verifiedIncidents() - before.verifiedIncidents()).isEqualTo(2);
        assertThat(galle.reportsByStatus().get("VERIFIED") - before.reportsByStatus().get("VERIFIED")).isEqualTo(2);
        assertThat(galle.provinceBreakdown())
                .as("only the one province is left once a district is chosen")
                .allSatisfy(row -> assertThat(row.name()).isEqualTo("Southern Province"));
    }

    @Test
    void withoutADistrictTheFiguresCoverTheWholeIsland() {
        SummaryResponse before = analytics.summary(null);
        report("Jaffna", "Northern Province", ReportStatus.VERIFIED);

        SummaryResponse after = analytics.summary("  ");

        assertThat(after.district()).isNull();
        assertThat(after.province()).isNull();
        assertThat(after.verifiedIncidents() - before.verifiedIncidents()).isEqualTo(1);
    }

    @Test
    void aDistrictThatDoesNotExistIsRefused() {
        assertThatThrownBy(() -> analytics.summary("Atlantis"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void report(String district, String province, ReportStatus status) {
        PollutionReport report = new PollutionReport();
        report.setReference("A-" + UUID.randomUUID());
        report.setTitle("Analytics test debris");
        report.setDescription("Analytics test report.");
        report.setSeverity(Severity.MEDIUM);
        report.setStatus(status);
        report.setLocationName(district);
        report.setDistrict(district);
        report.setProvince(province);
        report.setLatitude(6.0535);
        report.setLongitude(80.2210);
        report.setReporter(reporter());
        reports.save(report);
    }

    private User reporter() {
        User user = new User();
        user.setFullName("Analytics tester");
        user.setEmail(UUID.randomUUID() + "@test.lk");
        user.setPasswordHash("x");
        user.setRole(Role.CITIZEN);
        return users.save(user);
    }
}
