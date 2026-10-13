package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.ProjectDtos.EquipmentLine;
import lk.tideline.cleanup.dto.ProjectDtos.EquipmentNeed;
import lk.tideline.cleanup.dto.ProjectDtos.ProjectResponse;
import lk.tideline.cleanup.dto.ProjectDtos.ResourcesRequest;
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
import org.springframework.data.domain.PageRequest;
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
    private static final ResourcesRequest PLAN = new ResourcesRequest(20, 3, KIT, true);

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

        assertThat(alerts.findByRecipientOrderByCreatedAtDescIdDesc(admin))
                .filteredOn(alert -> alert.getType() == AlertType.AUTHORITY_DECISION)
                .singleElement()
                .satisfies(alert -> {
                    // Nothing for an administrator to do now that the officer assigns the resources.
                    assertThat(alert.isCritical()).isFalse();
                    assertThat(alert.getTitle()).startsWith("Government officer approved ");
                    assertThat(alert.getBody()).contains(NOTE);
                    assertThat(alert.getProjectId()).isEqualTo(decided.projectId());
                });

        assertThat(alerts.findByRecipientOrderByCreatedAtDescIdDesc(owner))
                .extracting(Alert::getTitle)
                .contains("Your report is now a project");
        assertThat(alerts.findByRecipientOrderByCreatedAtDescIdDesc(owner))
                .noneMatch(alert -> alert.getBody().contains(NOTE));

        assertThat(reportService.view(decided.id(), owner).authorityComment()).isNull();
        assertThat(reportService.view(decided.id(), admin).authorityComment()).isEqualTo(NOTE);
        assertThat(projects.view(decided.projectId(), owner).approval()).isNull();
        assertThat(projects.view(decided.projectId(), admin).approval().comment()).isEqualTo(NOTE);
    }

    @Test
    void aNewProjectHasNoResourcesUntilTheOfficerAssignsThem() {
        User owner = user(Role.CITIZEN);
        ReportResponse decided = approve(owner);

        assertThat(projects.view(decided.projectId(), owner).resources())
                .as("nothing to show the owner yet")
                .isNull();
        assertThat(alerts.findByRecipientOrderByCreatedAtDescIdDesc(owner))
                .singleElement()
                .satisfies(alert -> assertThat(alert.getTitle()).isEqualTo("Your report is now a project"));

        projects.updateResources(decided.projectId(), PLAN, user(Role.AUTHORITY));

        ProjectResponse seenByOwner = projects.view(decided.projectId(), owner);
        assertThat(seenByOwner.resources().finalized()).isTrue();
        assertThat(seenByOwner.resources().volunteersNeeded()).isEqualTo(20);
        assertThat(seenByOwner.resources().diversNeeded()).isEqualTo(3);
        assertThat(seenByOwner.resources().equipment()).extracting(EquipmentNeed::name).containsExactly("Gloves", "Boat");
        assertThat(alerts.findByRecipientOrderByCreatedAtDescIdDesc(owner))
                .extracting(Alert::getTitle)
                .contains("Resources assigned to " + seenByOwner.reference());
    }

    @Test
    void onlyTheOfficerAssignsResourcesAndAnEmptyPlanIsRefused() {
        User admin = user(Role.ADMIN);
        Long projectId = approve(user(Role.CITIZEN)).projectId();

        assertThatThrownBy(() -> projects.updateResources(projectId, PLAN, admin))
                .isInstanceOf(AccessDeniedException.class);

        ProjectResponse assigned = projects.updateResources(projectId, PLAN, user(Role.AUTHORITY));
        assertThat(assigned.resources().volunteersNeeded()).isEqualTo(20);
        assertThat(assigned.resources().finalized()).isTrue();

        assertThatThrownBy(() -> projects.updateResources(projectId,
                new ResourcesRequest(0, 0, List.of(), true), user(Role.AUTHORITY)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> projects.updateResources(projectId,
                new ResourcesRequest(0, 2, KIT, true), user(Role.AUTHORITY)))
                .as("divers and equipment are no substitute for people on the sand")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one volunteer");
    }

    @Test
    void nobodyJoinsACleanupBeforeTheOfficerHasAssignedItsResources() {
        User owner = user(Role.CITIZEN);
        User volunteer = user(Role.CITIZEN);
        Long projectId = approve(owner).projectId();

        assertThatThrownBy(() -> projects.join(projectId, volunteer, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not open yet");

        projects.updateResources(projectId, PLAN, user(Role.AUTHORITY));

        assertThat(projects.join(projectId, volunteer, null).volunteerCount()).isEqualTo(1);
    }

    @Test
    void anApprovalWithNoResourcesWaitsInItsOwnPartOfTheQueue() {
        PageRequest page = PageRequest.of(0, 500);
        Long projectId = approve(user(Role.CITIZEN)).projectId();
        Long reportId = projects.view(projectId, null).reportId();

        assertThat(reportService.awaitingResources(null, null, false, page).getContent())
                .extracting(ReportResponse::id)
                .contains(reportId);
        assertThat(reportService.search(null, null, null, true, false, user(Role.AUTHORITY), page).getContent())
                .as("and it is still in the officer's own listing, not lost")
                .extracting(ReportResponse::id)
                .contains(reportId);
        assertThat(reportService.search(null, null, null, true, false, user(Role.ADMIN), page).getContent())
                .as("resourcing is not the administrator's step, so it stays out of their queue")
                .extracting(ReportResponse::id)
                .doesNotContain(reportId);

        projects.updateResources(projectId, PLAN, user(Role.AUTHORITY));

        assertThat(reportService.awaitingResources(null, null, false, page).getContent())
                .extracting(ReportResponse::id)
                .doesNotContain(reportId);
    }

    @Test
    void aCleanupStopsTakingPeopleOnceItHasTheNumbersItAskedFor() {
        Long projectId = approve(user(Role.CITIZEN)).projectId();
        projects.updateResources(projectId, new ResourcesRequest(1, 0, KIT, true), user(Role.AUTHORITY));

        projects.join(projectId, user(Role.CITIZEN), null);

        assertThatThrownBy(() -> projects.join(projectId, user(Role.CITIZEN), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has everyone it needs");
        assertThatThrownBy(() -> projects.join(projectId, user(Role.DIVER), ParticipantRole.DIVER))
                .as("and the places it never asked for were never open")
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void someoneWhoDidNotPickARoleTakesWhicheverPlaceIsLeft() {
        Long projectId = approve(user(Role.CITIZEN)).projectId();
        projects.updateResources(projectId, new ResourcesRequest(1, 1, KIT, true), user(Role.AUTHORITY));
        projects.join(projectId, user(Role.DIVER), null);

        // A diver by trade, but the diving place is taken, so they fill the volunteer one.
        ProjectResponse joined = projects.join(projectId, user(Role.DIVER), null);

        assertThat(joined.volunteerCount()).isEqualTo(1);
        assertThat(joined.diverCount()).isEqualTo(1);
    }

    private ReportResponse approve(User reporter) {
        return reportService.decideAsAuthority(escalated(reporter).getId(),
                new AuthorityDecisionRequest(ReviewDecision.APPROVED, NOTE), user(Role.AUTHORITY));
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
