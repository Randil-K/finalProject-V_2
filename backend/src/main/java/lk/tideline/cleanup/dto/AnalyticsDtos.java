package lk.tideline.cleanup.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record SummaryResponse(
            /** When these figures were worked out, so a dashboard can say how fresh they are. */
            Instant generatedAt,
            /** The district these figures cover; null when they cover the whole island. */
            String district,
            /** The province that district belongs to — what the people counts cover. */
            String province,
            long reportedSites,
            /** Reports still waiting for an administrator to look at them. */
            long verifiedIncidents,
            /** Every report the community has ever verified, wherever it got to afterwards. */
            long communityVerified,
            long escalatedReports,
            long activeProjects,
            long completedProjects,
            long registeredVolunteers,
            /** Kilograms of debris recorded across every project. */
            double wasteRemovedKg,
            /** How many people have joined a cleanup. */
            long peopleJoined,
            /** Projects the officer has not yet said what they need. */
            long awaitingResources,
            /** Projects with everyone and everything they asked for. */
            long readyToRun,
            /** Divers, organisations and officials waiting to be verified. */
            long accountsAwaitingVerification,
            Map<String, Long> reportsByStatus,
            /** The same reports, each status split into the states behind it, for a stacked chart. */
            List<StatusColumn> reportStatusBreakdown,
            /** How many people joined a cleanup in each province. */
            List<RegionCount> participationByProvince,
            Map<String, Long> projectsByStage,
            /** How people answered the calls for help: joined, ignored, or not yet. */
            Map<String, Long> helpCallAnswers,
            List<RegionCount> topLocations,
            List<RegionCount> provinceBreakdown
    ) {
    }

    public record RegionCount(String name, long count) {
    }

    /** One column of the status chart: what it is called, and the parts it is made of. */
    public record StatusColumn(String label, List<RegionCount> parts) {
    }
}
