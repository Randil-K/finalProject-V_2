package lk.tideline.cleanup.service;

import lk.tideline.cleanup.dto.AnalyticsDtos.RegionCount;
import lk.tideline.cleanup.dto.AnalyticsDtos.StatusColumn;
import lk.tideline.cleanup.dto.AnalyticsDtos.SummaryResponse;
import lk.tideline.cleanup.model.AccountStatus;
import lk.tideline.cleanup.model.AlertReply;
import lk.tideline.cleanup.model.AlertType;
import lk.tideline.cleanup.model.ProjectStatus;
import lk.tideline.cleanup.model.ReportStatus;
import lk.tideline.cleanup.model.ReviewDecision;
import lk.tideline.cleanup.model.Role;
import lk.tideline.cleanup.repository.AlertRepository;
import lk.tideline.cleanup.repository.CleanupProjectRepository;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.ProjectParticipantRepository;
import lk.tideline.cleanup.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final PollutionReportRepository reportRepository;
    private final CleanupProjectRepository projectRepository;
    private final ProjectParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final AlertRepository alertRepository;

    public AnalyticsService(PollutionReportRepository reportRepository,
                            CleanupProjectRepository projectRepository,
                            ProjectParticipantRepository participantRepository,
                            UserRepository userRepository,
                            AlertRepository alertRepository) {
        this.reportRepository = reportRepository;
        this.projectRepository = projectRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary() {
        return summary(null);
    }

    /**
     * Every figure on the dashboard, narrowed to one district when one is named. A report carries its
     * own district and a project takes the district of the report it came from; accounts only ever
     * record a province, so the people counts cover the province that district belongs to.
     */
    @Transactional(readOnly = true)
    public SummaryResponse summary(String requested) {
        String district = requested == null || requested.isBlank() ? null
                : ContactDetails.requireKnownDistrict(requested.trim());
        String province = ContactDetails.provinceOfDistrict(district);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ReportStatus status : ReportStatus.values()) {
            if (status.becameProject()) {
                continue;
            }
            byStatus.put(status.name(), reportRepository.countInDistrictByStatus(status, district));
        }

        long planned = projectRepository.countInDistrictByStatus(ProjectStatus.PLANNED, district);
        long active = projectRepository.countInDistrictByStatus(ProjectStatus.ACTIVE, district);
        long completed = projectRepository.countInDistrictByStatus(ProjectStatus.COMPLETED, district);
        long awaitingResources =
                projectRepository.countInDistrictAwaitingResources(ProjectStatus.COMPLETED, district);
        long readyToRun =
                projectRepository.countInDistrictReadyToRun(ProjectStatus.COMPLETED, district);

        Map<String, Long> byStage = new LinkedHashMap<>();
        byStage.put("Waiting for resources", awaitingResources);
        byStage.put("Gathering people", Math.max(0, planned + active - awaitingResources - readyToRun));
        byStage.put("Ready to run", readyToRun);
        byStage.put("Completed", completed);

        long joined = alertRepository.countInDistrictByTypeAndReply(
                AlertType.HELP_NEEDED, AlertReply.JOINED, district);
        long ignored = alertRepository.countInDistrictByTypeAndReply(
                AlertType.HELP_NEEDED, AlertReply.IGNORED, district);
        Map<String, Long> answers = new LinkedHashMap<>();
        answers.put("Joined", joined);
        answers.put("Ignored", ignored);
        answers.put("No answer yet", Math.max(0,
                alertRepository.countInDistrictByType(AlertType.HELP_NEEDED, district) - joined - ignored));

        return new SummaryResponse(
                java.time.Instant.now(),
                district,
                province,
                reportRepository.countInDistrictByStatusNotIn(ReportStatus.BECAME_PROJECT, district),
                reportRepository.countInDistrictByStatus(ReportStatus.VERIFIED, district),
                reportRepository.countInDistrictByStatusIn(List.of(ReportStatus.VERIFIED, ReportStatus.ESCALATED,
                        ReportStatus.APPROVED, ReportStatus.CLEANED), district),
                reportRepository.countInDistrictByStatus(ReportStatus.ESCALATED, district),
                planned + active,
                completed,
                userRepository.countInProvinceByRoleIn(List.of(Role.CITIZEN, Role.DIVER), province),
                Math.round(projectRepository.totalDebrisRemovedKgInDistrict(district) * 10) / 10.0,
                participantRepository.countInDistrict(district),
                awaitingResources,
                readyToRun,
                userRepository.countInProvinceByAccountStatus(AccountStatus.PENDING_REVIEW, province),
                byStatus,
                statusBreakdown(byStatus, awaitingResources, district),
                toCounts(participantRepository.countGroupedByProjectProvince(district)),
                byStage,
                answers,
                toCounts(reportRepository.countGroupedByLocationIn(district)),
                toCounts(reportRepository.countGroupedByProvinceIn(district)));
    }

    /**
     * The status chart's columns. Most statuses are one thing, but two are worth breaking open: a
     * report sitting with the government officer is either waiting for their decision or waiting for
     * them to assign the cleanup its resources, and a rejected report was turned down by one of the
     * two reviewers. The community never rejects a report — losing its trust sends it back to
     * Verifying — so there is no community part to show.
     */
    private List<StatusColumn> statusBreakdown(Map<String, Long> byStatus, long awaitingResources,
                                               String district) {
        long rejected = byStatus.getOrDefault(ReportStatus.REJECTED.name(), 0L);
        long byAdmin = reportRepository.countInDistrictRejectedByAdmin(
                ReportStatus.REJECTED, ReviewDecision.REJECTED, district);
        long byOfficer = reportRepository.countInDistrictRejectedByAuthority(
                ReportStatus.REJECTED, ReviewDecision.REJECTED, district);
        long otherwise = Math.max(0, rejected - byAdmin - byOfficer);

        List<RegionCount> rejectedParts = new java.util.ArrayList<>();
        rejectedParts.add(new RegionCount("By an administrator", byAdmin));
        rejectedParts.add(new RegionCount("By the government officer", byOfficer));
        if (otherwise > 0) {
            rejectedParts.add(new RegionCount("Closed another way", otherwise));
        }

        return List.of(
                one("Pending", byStatus.getOrDefault(ReportStatus.PENDING.name(), 0L)),
                one("Verifying", byStatus.getOrDefault(ReportStatus.VERIFYING.name(), 0L)),
                one("Verified", byStatus.getOrDefault(ReportStatus.VERIFIED.name(), 0L)),
                new StatusColumn("With government officer", List.of(
                        new RegionCount("Waiting for approval",
                                byStatus.getOrDefault(ReportStatus.ESCALATED.name(), 0L)),
                        new RegionCount("Waiting for resource assigning", awaitingResources))),
                new StatusColumn("Rejected", List.copyOf(rejectedParts)));
    }

    private static StatusColumn one(String label, long count) {
        return new StatusColumn(label, List.of(new RegionCount(label, count)));
    }

    private List<RegionCount> toCounts(List<Object[]> rows) {
        return rows.stream()
                .filter(row -> row[0] != null)
                .map(row -> new RegionCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
