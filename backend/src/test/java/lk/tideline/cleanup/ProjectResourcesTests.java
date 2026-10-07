package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.ProjectDtos.EquipmentLine;
import lk.tideline.cleanup.dto.ProjectDtos.ProjectResponse;
import lk.tideline.cleanup.dto.ProjectDtos.ResourcesRequest;
import lk.tideline.cleanup.dto.ReportDtos.ApprovalResources;
import lk.tideline.cleanup.dto.ReportDtos.AuthorityDecisionRequest;
import lk.tideline.cleanup.dto.ReportDtos.ReportResponse;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.AlertRepository;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.ProjectService;
import lk.tideline.cleanup.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The government officer commits the resources a project needs as they approve it. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class ProjectResourcesTests {

    private static final String NOTE = "Needs 20 volunteers, 3 divers for the reef edge, and a boat.";
    private static final List<EquipmentLine> KIT = List.of(new EquipmentLine("Gloves", 40), new EquipmentLine("Boat", 1));
    private static final ApprovalResources RESOURCES = new ApprovalResources(20, 3, KIT);

    @Autowired
    private ReportService reportService;

    @Autowired
    private ProjectService projects;

    @Autowired
    private PollutionReportRepository reports;

    @Autowired
    private UserRepository users;

    @Autowired
    private AlertRepository alerts;

    @Test
    void theApprovalNoteReachesAdministratorsButNotTheOwner() {
        User admin = user(Role.ADMIN);
        User owner = user(Role.CITIZEN);
        ReportResponse decided = approve(owner);

        assertThat(alerts.findByRecipientOrderByCreatedAtDesc(admin))
                .filteredOn(alert -> alert.getType() == AlertType.AUTHORITY_DECISION)
                .singleElement()
                .satisfies(alert -> {
                    // Nothing for an administrator to do now that the officer assigns the resources.
                    assertThat(alert.isCritical()).isFalse();
                    assertThat(alert.getTitle()).startsWith("Government officer approved ");
                    assertThat(alert.getBody()).contains(NOTE);
                    assertThat(alert.getProjectId()).isEqualTo(decided.projectId());
                });

        assertThat(alerts.findByRecipientOrderByCreatedAtDesc(owner))
                .extracting(Alert::getTitle)
                .contains("Your report is now a project");
        assertThat(alerts.findByRecipientOrderByCreatedAtDesc(owner))
                .noneMatch(alert -> alert.getBody().contains(NOTE));

        assertThat(reportService.view(decided.id(), owner).authorityComment()).isNull();
        assertThat(reportService.view(decided.id(), admin).authorityComment()).isEqualTo(NOTE);
        assertThat(projects.view(decided.projectId(), owner).approval()).isNull();
        assertThat(projects.view(decided.projectId(), admin).approval().comment()).isEqualTo(NOTE);
    }

    @Test
    void approvingAssignsTheResourcesAndTellsTheOwnerWhatTheProjectHas() {
        User owner = user(Role.CITIZEN);
        ReportResponse decided = approve(owner);

        ProjectResponse seenByOwner = projects.view(decided.projectId(), owner);
        assertThat(seenByOwner.resources().finalized()).isTrue();
        assertThat(seenByOwner.resources().volunteersNeeded()).isEqualTo(20);
        assertThat(seenByOwner.resources().diversNeeded()).isEqualTo(3);
        assertThat(seenByOwner.resources().equipment()).extracting(EquipmentLine::name).containsExactly("Gloves", "Boat");

        // One alert, not a project alert followed by a separate resources alert.
        assertThat(alerts.findByRecipientOrderByCreatedAtDesc(owner))
                .singleElement()
                .satisfies(alert -> {
                    assertThat(alert.getTitle()).isEqualTo("Your report is now a project");
                    assertThat(alert.getBody()).contains("20 volunteers, 3 divers, 2 types of equipment");
                });
    }

    @Test
    void aReportCannotBeApprovedWithoutCommittingResources() {
        User owner = user(Role.CITIZEN);
        PollutionReport report = escalated(owner);

        assertThatThrownBy(() -> reportService.decideAsAuthority(report.getId(),
                new AuthorityDecisionRequest(ReviewDecision.APPROVED, NOTE, null), user(Role.AUTHORITY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volunteers, divers or equipment");

        assertThatThrownBy(() -> reportService.decideAsAuthority(report.getId(),
                new AuthorityDecisionRequest(ReviewDecision.APPROVED, NOTE,
                        new ApprovalResources(0, 0, List.of())), user(Role.AUTHORITY)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theOfficerCanReviseResourcesLaterButAnAdministratorCannot() {
        User admin = user(Role.ADMIN);
        Long projectId = approve(user(Role.CITIZEN)).projectId();
        ResourcesRequest revision = new ResourcesRequest(25, 4, KIT, true);

        assertThatThrownBy(() -> projects.updateResources(projectId, revision, admin))
                .isInstanceOf(AccessDeniedException.class);

        ProjectResponse revised = projects.updateResources(projectId, revision, user(Role.AUTHORITY));
        assertThat(revised.resources().volunteersNeeded()).isEqualTo(25);
        assertThat(revised.resources().finalized()).isTrue();

        assertThatThrownBy(() -> projects.updateResources(projectId,
                new ResourcesRequest(0, 0, List.of(), true), user(Role.AUTHORITY)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ReportResponse approve(User reporter) {
        return reportService.decideAsAuthority(escalated(reporter).getId(),
                new AuthorityDecisionRequest(ReviewDecision.APPROVED, NOTE, RESOURCES), user(Role.AUTHORITY));
    }

    private PollutionReport escalated(User reporter) {
        PollutionReport report = new PollutionReport();
        report.setReference("T-" + UUID.randomUUID());
        report.setTitle("Ghost nets");
        report.setDescription("Nets on the reef.");
        report.setSeverity(Severity.HIGH);
        report.setStatus(ReportStatus.ESCALATED);
        report.setAdminDecision(ReviewDecision.APPROVED);
        report.setAuthorityDecision(ReviewDecision.PENDING);
        report.setLocationName("Kalpitiya");
        report.setProvince("North Western Province");
        report.setLatitude(8.23);
        report.setLongitude(79.76);
        report.setReporter(reporter);
        return reports.save(report);
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
