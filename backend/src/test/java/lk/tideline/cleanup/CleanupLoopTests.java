package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.ProjectDtos.ProjectResponse;
import lk.tideline.cleanup.dto.ProjectDtos.ProjectUpdateRequest;
import lk.tideline.cleanup.dto.ProjectDtos.EquipmentLine;
import lk.tideline.cleanup.dto.ReportDtos.ApprovalResources;
import lk.tideline.cleanup.dto.ReportDtos.AuthorityDecisionRequest;
import lk.tideline.cleanup.dto.ReportDtos.ReportResponse;
import lk.tideline.cleanup.dto.UserDtos.OwnedProject;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.AlertRepository;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.ProjectService;
import lk.tideline.cleanup.service.ReportService;
import lk.tideline.cleanup.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Authority approval → project owned by the reporter → cleaned, end to end through the service layer. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class CleanupLoopTests {

    /** Every approval commits the resources the cleanup needs. */
    private static final ApprovalResources RESOURCES =
            new ApprovalResources(10, 2, List.of(new EquipmentLine("Gloves", 20)));

    @Autowired
    private UserRepository users;

    @Autowired
    private PollutionReportRepository reports;

    @Autowired
    private AlertRepository alerts;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ProjectService projects;

    @Autowired
    private UserService userService;

    @Test
    void authorityApprovalTurnsTheReportIntoAProjectOwnedByTheReporter() {
        User reporter = user(Role.CITIZEN);
        PollutionReport report = escalatedReport(reporter);

        ReportResponse decided = reportService.decideAsAuthority(report.getId(),
                new AuthorityDecisionRequest(ReviewDecision.APPROVED, "Approved.", RESOURCES), user(Role.AUTHORITY));

        assertThat(decided.status()).isEqualTo(ReportStatus.APPROVED);
        assertThat(decided.authorityDecision()).isEqualTo(ReviewDecision.APPROVED);
        assertThat(decided.projectId()).isNotNull();

        ProjectResponse project = projects.view(decided.projectId(), reporter);
        assertThat(project.owner().id()).isEqualTo(reporter.getId());
        assertThat(project.reportId()).isEqualTo(report.getId());
        assertThat(project.status()).isEqualTo(ProjectStatus.PLANNED);

        assertThat(userService.view(reporter.getId()).ownedProjects())
                .extracting(OwnedProject::id)
                .containsExactly(project.id());
        assertThat(titlesFor(reporter)).contains("Your report is now a project");
    }

    @Test
    void completingTheProjectMarksTheReportCleaned() {
        User reporter = user(Role.CITIZEN);
        User volunteer = user(Role.CITIZEN);
        ProjectResponse project = approve(reporter);

        projects.join(project.id(), volunteer, null);
        projects.addUpdate(project.id(),
                new ProjectUpdateRequest(UpdateStage.AFTER, "All clear.", null, 100, 12.5), reporter);

        assertThat(reports.findById(project.reportId()).orElseThrow().getStatus()).isEqualTo(ReportStatus.CLEANED);
        assertThat(titlesFor(volunteer)).contains("Cleanup complete");
        // The owner hears about their own project finishing, in their own words.
        assertThat(titlesFor(reporter)).containsOnlyOnce("Your cleanup is complete");
        assertThat(titlesFor(reporter)).doesNotContain("Cleanup complete");
    }

    @Test
    void onlyTheProjectOwnerCanRecordProgress() {
        User reporter = user(Role.CITIZEN);
        ProjectResponse project = approve(reporter);
        ProjectUpdateRequest update = new ProjectUpdateRequest(UpdateStage.DURING, "Half done.", null, 50, 3.0);

        for (Role official : List.of(Role.AUTHORITY, Role.ADMIN, Role.CITIZEN)) {
            assertThatThrownBy(() -> projects.addUpdate(project.id(), update, user(official)))
                    .as(official + " cannot post progress")
                    .isInstanceOf(AccessDeniedException.class);
        }
        assertThat(projects.addUpdate(project.id(), update, reporter).completionPercentage()).isEqualTo(50);
    }

    @Test
    void anApprovedReportIsNoLongerListedAsAReport() {
        User reporter = user(Role.CITIZEN);
        PollutionReport open = escalatedReport(reporter);
        ProjectResponse project = approve(reporter);
        PageRequest page = PageRequest.of(0, 500);

        assertThat(reportService.search(null, null, null, page).getContent())
                .extracting(ReportResponse::id)
                .contains(open.getId())
                .doesNotContain(project.reportId());
        assertThat(reportService.search(ReportStatus.APPROVED, null, null, page).getContent()).isEmpty();
    }

    @Test
    void anApprovedReportCannotBeDecidedAgainOrVotedOn() {
        User reporter = user(Role.CITIZEN);
        ProjectResponse project = approve(reporter);

        assertThatThrownBy(() -> reportService.decideAsAuthority(project.reportId(),
                new AuthorityDecisionRequest(ReviewDecision.APPROVED, "Again.", RESOURCES), user(Role.AUTHORITY)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> reportService.vote(project.reportId(), user(Role.CITIZEN), true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void theOwnerCannotJoinTheirOwnProject() {
        User reporter = user(Role.CITIZEN);
        ProjectResponse project = approve(reporter);

        assertThatThrownBy(() -> projects.join(project.id(), reporter, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theOwnerRatesContributionsOnceTheProjectIsComplete() {
        User owner = user(Role.CITIZEN);
        User volunteer = user(Role.DIVER);
        ProjectResponse project = approve(owner);
        projects.join(project.id(), volunteer, null);

        assertThat(projects.view(project.id(), volunteer).participants())
                .as("participants stay private to the owner")
                .isNull();
        Long participantId = projects.view(project.id(), owner).participants().get(0).id();

        assertThatThrownBy(() -> projects.mark(project.id(), participantId, 4, owner))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("complete");

        projects.addUpdate(project.id(),
                new ProjectUpdateRequest(UpdateStage.AFTER, "Done.", null, 100, null), owner);

        assertThatThrownBy(() -> projects.mark(project.id(), participantId, 4, volunteer))
                .isInstanceOf(IllegalStateException.class);

        projects.mark(project.id(), participantId, 4, owner);
        assertThat(userService.view(volunteer.getId()).averageMark()).isEqualTo(4.0);
        assertThat(titlesFor(volunteer)).contains("Your contribution was rated");
    }

    private ProjectResponse approve(User reporter) {
        PollutionReport report = escalatedReport(reporter);
        ReportResponse decided = reportService.decideAsAuthority(report.getId(),
                new AuthorityDecisionRequest(ReviewDecision.APPROVED, "Approved.", RESOURCES), user(Role.AUTHORITY));
        return projects.view(decided.projectId(), reporter);
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

    private PollutionReport escalatedReport(User reporter) {
        PollutionReport report = new PollutionReport();
        report.setReference("T-" + UUID.randomUUID());
        report.setTitle("Test debris");
        report.setDescription("Test report.");
        report.setSeverity(Severity.MEDIUM);
        report.setStatus(ReportStatus.ESCALATED);
        report.setAdminDecision(ReviewDecision.APPROVED);
        report.setAuthorityDecision(ReviewDecision.PENDING);
        report.setLocationName("Negombo");
        report.setProvince("Western Province");
        report.setLatitude(7.2083);
        report.setLongitude(79.8358);
        report.setReporter(reporter);
        return reports.save(report);
    }
}
