package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.AlertDtos.AlertReplyRequest;
import lk.tideline.cleanup.dto.ProjectDtos.EquipmentLine;
import lk.tideline.cleanup.dto.ProjectDtos.ProjectResponse;
import lk.tideline.cleanup.dto.ProjectDtos.ResourcesRequest;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.AlertRepository;
import lk.tideline.cleanup.repository.CleanupProjectRepository;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.AlertService;
import lk.tideline.cleanup.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Module 6 — calling for help within 15 km, widening to 25 km, and filling what a cleanup needs. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class RecruitmentTests {

    /** Galle, and three distances from it: inside 15 km, in the 15-25 km ring, and well beyond. */
    private static final double SITE_LAT = 6.0300;
    private static final double SITE_LON = 80.2200;

    @Autowired
    private ProjectService projects;

    @Autowired
    private AlertService alerts;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CleanupProjectRepository projectRepository;

    @Autowired
    private PollutionReportRepository reports;

    @Autowired
    private UserRepository users;

    @Test
    void settingTheResourcesAsksEveryoneWithinFifteenKilometres() {
        User near = userAt(6.0600, 80.2200);      // about 3 km away
        User ring = userAt(6.2100, 80.2200);      // about 20 km away
        User faraway = userAt(7.2000, 79.8500);   // Negombo

        CleanupProject project = approvedProject();

        assertThat(callsFor(project, near)).hasSize(1);
        assertThat(callsFor(project, near).get(0).getRadiusKm()).isEqualTo(15.0);
        assertThat(callsFor(project, near).get(0).getTitle()).startsWith("Help needed at ");
        assertThat(callsFor(project, ring)).isEmpty();
        assertThat(callsFor(project, faraway)).isEmpty();
        assertThat(projectRepository.findById(project.getId()).orElseThrow().getRecruitmentRadiusKm())
                .isEqualTo(15.0);
    }

    @Test
    void wideningAsksTheRingWithoutAskingAnyoneTwice() {
        User near = userAt(6.0600, 80.2200);
        User ring = userAt(6.2100, 80.2200);
        User faraway = userAt(7.2000, 79.8500);
        CleanupProject project = approvedProject();

        projects.widenCallForHelp(project.getId());

        assertThat(callsFor(project, ring)).hasSize(1);
        assertThat(callsFor(project, ring).get(0).getRadiusKm()).isEqualTo(25.0);
        assertThat(callsFor(project, ring).get(0).getTitle()).startsWith("Still looking for help");
        assertThat(callsFor(project, near)).as("already asked at 15 km").hasSize(1);
        assertThat(callsFor(project, faraway)).isEmpty();
        assertThat(projectRepository.findById(project.getId()).orElseThrow().getRecruitmentWidenedAt()).isNotNull();
    }

    @Test
    void joiningFromTheAlertCountsTowardsWhatTheCleanupNeeds() {
        User near = userAt(6.0600, 80.2200);
        CleanupProject project = approvedProject();
        Alert call = callsFor(project, near).get(0);

        alerts.reply(call.getId(), new AlertReplyRequest(AlertReply.JOINED,
                List.of(new EquipmentLine("Gloves", 2))), near);

        ProjectResponse seen = projects.view(project.getId(), near);
        assertThat(seen.volunteerCount()).isEqualTo(1);
        assertThat(seen.joined()).isTrue();
        assertThat(seen.resources().equipment())
                .singleElement()
                .satisfies(item -> assertThat(item.securedQuantity()).isEqualTo(2));
        assertThat(alertRepository.findById(call.getId()).orElseThrow().getReply()).isEqualTo(AlertReply.JOINED);
    }

    @Test
    void ignoringRecordsTheAnswerAndLeavesTheCleanupAlone() {
        User near = userAt(6.0600, 80.2200);
        CleanupProject project = approvedProject();
        Alert call = callsFor(project, near).get(0);

        alerts.reply(call.getId(), new AlertReplyRequest(AlertReply.IGNORED, null), near);

        assertThat(projects.view(project.getId(), near).volunteerCount()).isZero();
        assertThat(alertRepository.findById(call.getId()).orElseThrow().getReply()).isEqualTo(AlertReply.IGNORED);

        assertThatThrownBy(() -> alerts.reply(call.getId(),
                new AlertReplyRequest(AlertReply.JOINED, null), near))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already answered");
    }

    @Test
    void theAdministratorsHearOnceEverythingIsPledged() {
        User admin = user(Role.ADMIN, null, null);
        User near = userAt(6.0600, 80.2200);
        CleanupProject project = approvedProject();
        Alert call = callsFor(project, near).get(0);

        alerts.reply(call.getId(), new AlertReplyRequest(AlertReply.JOINED,
                List.of(new EquipmentLine("Gloves", 4))), near);

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getResourcesGatheredAt()).isNotNull();
        assertThat(alertRepository.findByRecipientOrderByCreatedAtDesc(admin))
                .filteredOn(alert -> alert.getType() == AlertType.RESOURCES_GATHERED)
                .singleElement()
                .satisfies(alert -> assertThat(alert.getTitle()).startsWith("Resources gathered for "));
        assertThat(alertRepository.findByRecipientOrderByCreatedAtDesc(project.getOwner()))
                .extracting(Alert::getTitle)
                .contains("Your cleanup has everything it needs");
    }

    /** One volunteer and four gloves, so a single person joining can fill it. */
    private CleanupProject approvedProject() {
        PollutionReport report = new PollutionReport();
        report.setReference("T-" + UUID.randomUUID());
        report.setTitle("Plastic along the bay");
        report.setDescription("Bottles and bags.");
        report.setSeverity(Severity.MEDIUM);
        report.setStatus(ReportStatus.APPROVED);
        report.setLocationName("Galle");
        report.setProvince("Southern Province");
        report.setLatitude(SITE_LAT);
        report.setLongitude(SITE_LON);
        report.setReporter(user(Role.CITIZEN, null, null));
        reports.save(report);

        CleanupProject project = projects.createFromApprovedReport(report);
        // The call for help goes out when the government officer assigns what the cleanup needs.
        projects.updateResources(project.getId(),
                new ResourcesRequest(1, 0, List.of(new EquipmentLine("Gloves", 4)), true),
                user(Role.AUTHORITY, null, null));
        return project;
    }

    private List<Alert> callsFor(CleanupProject project, User user) {
        return alertRepository.findByRecipientOrderByCreatedAtDesc(user).stream()
                .filter(alert -> alert.getType() == AlertType.HELP_NEEDED)
                .filter(alert -> project.getId().equals(alert.getProjectId()))
                .toList();
    }

    private User userAt(double latitude, double longitude) {
        return user(Role.CITIZEN, latitude, longitude);
    }

    private User user(Role role, Double latitude, Double longitude) {
        User user = new User();
        user.setFullName(role.name() + " " + UUID.randomUUID().toString().substring(0, 6));
        user.setEmail(UUID.randomUUID() + "@test.lk");
        user.setPasswordHash("x");
        user.setRole(role);
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        return users.save(user);
    }
}
