package lk.tideline.cleanup.model;

public enum AlertType {
    NEW_REPORT_NEARBY,
    REPORT_VERIFIED,
    ALERT_ESCALATED,
    PROJECT_PLANNED,
    PROJECT_UPDATE,
    AUTHORITY_DECISION,
    OPPORTUNITY,
    /** The decision on the recipient's own registration. */
    ACCOUNT_REVIEW,
    /** Someone else's registration waiting for an administrator to approve it. */
    ACCOUNT_APPLICATION,
    COMMENT_REPLY,
    INFO_REQUESTED,
    INFO_RESPONSE,
    RESOURCES_NEEDED,
    RESOURCES_ASSIGNED,
    /** A call for help sent to people living near a cleanup that still needs hands. */
    HELP_NEEDED,
    /** Everything a cleanup needs has been pledged. */
    RESOURCES_GATHERED
}
