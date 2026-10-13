package lk.tideline.cleanup.model;

import java.util.List;
import java.util.Set;

/**
 * Report lifecycle. A report is submitted (PENDING), put to the community (VERIFYING), passes the
 * trust threshold (VERIFIED), is approved by an administrator and sent to the government authority
 * (ESCALATED), and approved there (APPROVED) — at which point it has become a cleanup project.
 * It can be closed along the way (REJECTED), and ends CLEANED when its project is complete.
 */
public enum ReportStatus {
    PENDING,
    VERIFYING,
    VERIFIED,
    ESCALATED,
    APPROVED,
    REJECTED,
    CLEANED;

    /** Once approved by the authority a report lives on as a project and is no longer listed as a report. */
    public static final Set<ReportStatus> BECAME_PROJECT = Set.of(APPROVED, CLEANED);

    /** Kept out of the feed's "All": projects have their own pages, and rejections have their own filter. */
    public static final Set<ReportStatus> HIDDEN_FROM_FEED = Set.of(APPROVED, CLEANED, REJECTED);

    /** What the administrators' review queue lists: verified by the community, with the authority, or rejected. */
    public static final List<ReportStatus> REVIEW_QUEUE = List.of(VERIFIED, ESCALATED, REJECTED);

    public boolean becameProject() {
        return BECAME_PROJECT.contains(this);
    }
}
