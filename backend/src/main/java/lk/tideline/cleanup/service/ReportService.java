package lk.tideline.cleanup.service;

import lk.tideline.cleanup.config.TidelineProperties;
import lk.tideline.cleanup.dto.ReportDtos.*;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.CleanupProjectRepository;
import lk.tideline.cleanup.repository.CommentReactionRepository;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.ReportCommentRepository;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.repository.VerificationVoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Entities are mapped to DTOs inside the transaction — lazy associations such as the
 * reporter and the photo list cannot be read once the session has closed.
 */
@Service
public class ReportService {

    private final PollutionReportRepository reportRepository;
    private final VerificationVoteRepository voteRepository;
    private final ReportCommentRepository commentRepository;
    private final CommentReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final CleanupProjectRepository projectRepository;
    private final ProjectService projectService;
    private final AlertService alertService;
    private final TidelineProperties properties;
    private final DocumentStorageService storage;
    private final InfoRequestService infoRequests;

    public ReportService(PollutionReportRepository reportRepository,
                         VerificationVoteRepository voteRepository,
                         ReportCommentRepository commentRepository,
                         CommentReactionRepository reactionRepository,
                         UserRepository userRepository,
                         CleanupProjectRepository projectRepository,
                         ProjectService projectService,
                         AlertService alertService,
                         TidelineProperties properties,
                         DocumentStorageService storage,
                         InfoRequestService infoRequests) {
        this.reportRepository = reportRepository;
        this.voteRepository = voteRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.alertService = alertService;
        this.properties = properties;
        this.storage = storage;
        this.infoRequests = infoRequests;
    }

    private int minimumConfirmations() {
        return properties.getVerification().getMinimumConfirmations();
    }

    private int thresholdPercent() {
        return properties.getVerification().getThresholdPercent();
    }

    private ReportResponse toResponse(PollutionReport report) {
        return toResponse(report, null);
    }

    /** With a viewer, includes how they voted so the page can show their choice. */
    private ReportResponse toResponse(PollutionReport report, User viewer) {
        CleanupProject project = projectRepository.findFirstByReportId(report.getId()).orElse(null);
        Boolean myVote = viewer == null ? null : voteRepository.findByReportAndVoter(report, viewer)
                .map(VerificationVote::isConfirmed)
                .orElse(null);
        // The officer's approval note is for administrators and officers only.
        boolean reviewer = viewer != null && (viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.AUTHORITY);
        boolean hideNote = !reviewer && report.getAuthorityDecision() == ReviewDecision.APPROVED;
        return ReportResponse.from(report, thresholdPercent(), minimumConfirmations(), project,
                infoRequests.latestStatus(report), myVote, hideNote);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> search(ReportStatus status, Severity severity, String province, Pageable pageable) {
        return search(status, severity, province, false, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> search(ReportStatus status, Severity severity, String province, boolean reviewQueue,
                                       Pageable pageable) {
        // Approved reports are projects now; they're found through the projects API instead.
        if (status != null && status.becameProject()) {
            return Page.empty(pageable);
        }
        if (reviewQueue) {
            // Reports reach the administrators only once the community has verified them.
            if (status != null && !ReportStatus.REVIEW_QUEUE.contains(status)) {
                return Page.empty(pageable);
            }
            return reportRepository.searchIn(status == null ? ReportStatus.REVIEW_QUEUE : List.of(status),
                    severity, province, pageable).map(this::toResponse);
        }
        return reportRepository.search(status, ReportStatus.BECAME_PROJECT, severity, province, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReportResponse view(Long id) {
        return toResponse(get(id));
    }

    @Transactional(readOnly = true)
    public ReportResponse view(Long id, User viewer) {
        return toResponse(get(id), viewer);
    }

    private PollutionReport get(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report " + id + " was not found."));
    }

    @Transactional
    public ReportResponse create(CreateReportRequest request, User reporter) {
        return create(request, List.of(), reporter);
    }

    @Transactional
    public ReportResponse create(CreateReportRequest request, List<MultipartFile> evidence, User reporter) {
        List<DocumentStorageService.CheckedFile> files = storage.checkEvidence(evidence);
        PollutionReport report = new PollutionReport();
        report.setReference("TMP-" + UUID.randomUUID());
        report.setTitle(request.title());
        report.setDescription(request.description());
        report.setSeverity(request.severity());
        report.setLocationName(request.locationName());
        report.setProvince(request.province());
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setReporter(reporter);
        report.setStatus(ReportStatus.PENDING);

        if (request.photoUrls() != null) {
            for (String url : request.photoUrls()) {
                ReportPhoto photo = new ReportPhoto();
                photo.setReport(report);
                photo.setUrl(url);
                report.getPhotos().add(photo);
            }
        }
        for (DocumentStorageService.CheckedFile file : files) {
            String storedName = storage.saveEvidence(file);
            ReportPhoto photo = new ReportPhoto();
            photo.setReport(report);
            photo.setStoredName(storedName);
            photo.setContentType(file.contentType());
            photo.setUrl("/api/reports/evidence/" + storedName);
            photo.setCaption(file.originalName());
            report.getPhotos().add(photo);
        }

        PollutionReport saved = reportRepository.saveAndFlush(report);
        saved.setReference("SR-" + (2400 + saved.getId()));

        double radius = properties.getAlerts().getInitialRadiusKm();
        saved.setAlertRadiusKm(radius);
        alertService.notifyNearby(saved, radius,
                AlertType.NEW_REPORT_NEARBY,
                "New pollution report near " + saved.getLocationName(),
                saved.getTitle() + " — the community can now confirm or dispute this report.");

        return toResponse(saved);
    }

    /**
     * Records a community vote and recalculates the trust percentage. A report that reaches
     * the configured threshold (75% in the SRS) with enough votes becomes Verified.
     */
    @Transactional
    public ReportResponse vote(Long reportId, User voter, boolean confirmed) {
        PollutionReport report = get(reportId);

        if (report.getStatus() == ReportStatus.REJECTED
                || report.getStatus() == ReportStatus.APPROVED
                || report.getStatus() == ReportStatus.CLEANED) {
            throw new IllegalStateException("Voting is closed for this report.");
        }
        if (report.getReporter().getId().equals(voter.getId())) {
            throw new IllegalStateException("You can't vote on your own report.");
        }

        VerificationVote existing = voteRepository.findByReportAndVoter(report, voter).orElse(null);
        if (existing != null && existing.isConfirmed() == confirmed) {
            // Choosing the same option again takes the vote back.
            voteRepository.delete(existing);
            voteRepository.flush();
        } else {
            VerificationVote vote = existing != null ? existing : new VerificationVote();
            vote.setReport(report);
            vote.setVoter(voter);
            vote.setConfirmed(confirmed);
            voteRepository.saveAndFlush(vote);
        }

        return toResponse(recalculateTrust(report), voter);
    }

    private PollutionReport recalculateTrust(PollutionReport report) {
        int confirm = (int) voteRepository.countByReportAndConfirmed(report, true);
        int dispute = (int) voteRepository.countByReportAndConfirmed(report, false);
        int total = confirm + dispute;
        int percentage = total == 0 ? 0 : Math.round((confirm * 100f) / total);

        report.setConfirmVotes(confirm);
        report.setDisputeVotes(dispute);
        report.setTrustPercentage(percentage);
        report.setUpdatedAt(Instant.now());

        if (report.getStatus() == ReportStatus.PENDING && total > 0) {
            report.setStatus(ReportStatus.VERIFYING);
        }

        boolean threshold = percentage >= thresholdPercent() && confirm >= minimumConfirmations();

        // A withdrawn vote can take a report back below the threshold, as long as no administrator has acted on it.
        if (report.getStatus() == ReportStatus.VERIFIED && !threshold && report.getAdminDecision() == ReviewDecision.PENDING) {
            report.setStatus(ReportStatus.VERIFYING);
            report.setVerifiedAt(null);
        }
        if (report.getStatus() == ReportStatus.VERIFYING && total == 0) {
            report.setStatus(ReportStatus.PENDING);
        }

        if (report.getStatus() == ReportStatus.VERIFYING && threshold) {
            report.setStatus(ReportStatus.VERIFIED);
            report.setVerifiedAt(Instant.now());
            alertService.send(report.getReporter(), AlertType.REPORT_VERIFIED,
                    "Your report was verified",
                    report.getReference() + " was confirmed by " + confirm + " people (" + percentage
                            + "% trust). An administrator reviews it next.",
                    report.getId(), null, null);
            // Community verification hands the report to the administrators.
            for (User admin : userRepository.findByRole(Role.ADMIN)) {
                alertService.send(admin, AlertType.REPORT_VERIFIED,
                        "Report ready for your review",
                        report.getReference() + " at " + report.getLocationName() + " was confirmed by " + confirm
                                + " people (" + percentage + "% trust). Approve it, reject it, or ask the reporter for more information.",
                        report.getId(), null, null);
            }
        }

        return report;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long reportId, User viewer) {
        PollutionReport report = get(reportId);
        Map<Long, List<CommentReaction>> reactions = reactionRepository.findByReport(report).stream()
                .collect(Collectors.groupingBy(reaction -> reaction.getComment().getId()));
        return commentRepository.findByReportOrderByCreatedAtAsc(report).stream()
                .map(comment -> CommentResponse.from(comment, reactions.getOrDefault(comment.getId(), List.of()), viewer))
                .toList();
    }

    @Transactional
    public CommentResponse comment(Long reportId, User author, CommentRequest request) {
        PollutionReport report = get(reportId);
        ReportComment comment = new ReportComment();
        comment.setReport(report);
        comment.setAuthor(author);
        comment.setBody(request.body().trim());
        comment.setOfficial(author.getRole() == Role.ADMIN || author.getRole() == Role.AUTHORITY);

        if (request.parentId() != null) {
            ReportComment parent = comment(report, request.parentId());
            // Keep threads one level deep, like replies on a social post.
            comment.setParent(parent.getParent() != null ? parent.getParent() : parent);
            notifyReply(parent, author, report);
        }
        return CommentResponse.from(commentRepository.save(comment), List.of(), author);
    }

    /** Toggles a reaction: the same type again removes it, a different type replaces it. */
    @Transactional
    public CommentResponse react(Long reportId, Long commentId, ReactionType type, User user) {
        ReportComment comment = comment(get(reportId), commentId);
        reactionRepository.findByCommentAndUser(comment, user).ifPresentOrElse(existing -> {
            if (existing.getType() == type) {
                reactionRepository.delete(existing);
            } else {
                existing.setType(type);
            }
        }, () -> {
            CommentReaction reaction = new CommentReaction();
            reaction.setComment(comment);
            reaction.setUser(user);
            reaction.setType(type);
            reactionRepository.save(reaction);
        });
        reactionRepository.flush();
        return CommentResponse.from(comment, reactionRepository.findByComment(comment), user);
    }

    private ReportComment comment(PollutionReport report, Long commentId) {
        return commentRepository.findById(commentId)
                .filter(found -> found.getReport().getId().equals(report.getId()))
                .orElseThrow(() -> new NotFoundException("That comment was not found."));
    }

    private void notifyReply(ReportComment parent, User author, PollutionReport report) {
        User recipient = parent.getAuthor();
        if (recipient.getId().equals(author.getId())) {
            return;
        }
        String body = parent.getBody();
        String preview = body.length() > 80 ? body.substring(0, 77) + "..." : body;
        alertService.send(recipient, AlertType.COMMENT_REPLY,
                author.getFullName() + " replied to your comment",
                "On " + report.getReference() + ": \"" + preview + "\"",
                report.getId(), null, null);
    }

    /**
     * Module 4 — the administrator approves a report (sending it to the government authority),
     * rejects it, or asks the reporter for more information.
     */
    @Transactional
    public ReportResponse moderate(Long reportId, ModerationRequest request, User admin) {
        PollutionReport report = get(reportId);

        switch (report.getStatus()) {
            case ESCALATED -> throw new IllegalStateException(
                    "This report is with the government officer. Wait for their decision.");
            case APPROVED, CLEANED -> throw new IllegalStateException("This report has already become a project.");
            case REJECTED -> throw new IllegalStateException("This report was rejected and is closed.");
            default -> {
            }
        }

        ReviewDecision decision = request.decision();
        String comment = trimmed(request.comment());
        Instant now = Instant.now();
        String reference = report.getReference();

        // Reviewers can remove a false report at any time, but approval and questions wait for the community.
        if (decision != ReviewDecision.REJECTED && report.getStatus() != ReportStatus.VERIFIED) {
            throw new IllegalStateException("This report needs " + minimumConfirmations() + " confirmations and "
                    + thresholdPercent() + "% community trust before an administrator can approve it or ask for more information.");
        }

        switch (decision) {
            case APPROVED -> {
                report.setStatus(ReportStatus.ESCALATED);
                report.setEscalatedAt(now);
                report.setAuthorityDecision(ReviewDecision.PENDING);
                alertService.send(report.getReporter(), AlertType.AUTHORITY_DECISION,
                        "An administrator approved your report",
                        reference + " has been sent to the government officer for a decision.",
                        report.getId(), null, null);
                for (User officer : userRepository.findByRole(Role.AUTHORITY)) {
                    if (officer.isSuspended()) {
                        continue;
                    }
                    alertService.send(officer, AlertType.AUTHORITY_DECISION,
                            "Report escalated for your review",
                            reference + " at " + report.getLocationName() + " — " + report.getTrustPercentage()
                                    + "% community trust. Approve it to create a cleanup project, reject it, or ask for more detail.",
                            report.getId(), null, null);
                }
            }
            case MORE_INFO_REQUESTED -> {
                require(comment, "Say what information you need. The reporter sees your request.");
                infoRequests.open(report, admin, comment);
            }
            case REJECTED -> {
                require(comment, "Give a reason for rejecting this report. The reporter sees it.");
                report.setStatus(ReportStatus.REJECTED);
                alertService.send(report.getReporter(), AlertType.AUTHORITY_DECISION,
                        "Your report was rejected",
                        reference + ": " + comment, report.getId(), null, null);
            }
            default -> throw new IllegalArgumentException("Choose approve, reject, or request more information.");
        }

        report.setAdminDecision(decision);
        report.setModerationComment(comment);
        report.setAdminReviewedAt(now);
        report.setUpdatedAt(now);
        return toResponse(report);
    }

    /**
     * Module 5 — the government officer approves, rejects, or asks for more information. Approval
     * finishes the report: it becomes a cleanup project owned by the person who reported it.
     */
    @Transactional
    public ReportResponse decideAsAuthority(Long reportId, AuthorityDecisionRequest request, User officer) {
        PollutionReport report = get(reportId);

        if (report.getStatus() != ReportStatus.ESCALATED) {
            throw new IllegalStateException("Only a report an administrator has sent to the government officer can be decided.");
        }
        ReviewDecision decision = request.decision();
        if (decision == ReviewDecision.PENDING) {
            throw new IllegalArgumentException("Choose approve, reject, or request more information.");
        }

        String comment = request.comment().trim();
        Instant now = Instant.now();
        String reference = report.getReference();

        report.setAuthorityOfficer(officer);
        report.setAuthorityDecision(decision);
        report.setAuthorityComment(comment);
        report.setDecidedAt(now);
        report.setUpdatedAt(now);

        String adminTitle;
        switch (decision) {
            case APPROVED -> {
                report.setStatus(ReportStatus.APPROVED);
                CleanupProject project = projectService.createFromApprovedReport(report);

                alertService.send(report.getReporter(), AlertType.PROJECT_PLANNED,
                        "Your report is now a project",
                        "The government officer approved " + reference + ". It is now project "
                                + project.getReference() + " and you are its project owner.",
                        report.getId(), project.getId(), null);
                // The officer's note is for the administrators keeping an eye on the workflow, not for the owner.
                for (User admin : userRepository.findByRole(Role.ADMIN)) {
                    alertService.send(admin, AlertType.AUTHORITY_DECISION,
                            "Government officer approved " + reference,
                            officer.getFullName() + ": " + comment
                                    + " — " + project.getReference() + " is waiting for its resources.",
                            report.getId(), project.getId(), null);
                }
                adminTitle = null;
            }
            case REJECTED -> {
                report.setStatus(ReportStatus.REJECTED);
                alertService.send(report.getReporter(), AlertType.AUTHORITY_DECISION,
                        "The government officer rejected your report",
                        reference + ": " + comment, report.getId(), null, null);
                adminTitle = "Government officer rejected " + reference;
            }
            default -> {
                infoRequests.open(report, officer, comment);
                adminTitle = "Government officer requested more information on " + reference;
            }
        }

        // Module 5 — "notify admin about approval status" (approval sends its own alert above).
        for (User admin : adminTitle == null ? List.<User>of() : userRepository.findByRole(Role.ADMIN)) {
            alertService.send(admin, AlertType.AUTHORITY_DECISION, adminTitle,
                    officer.getFullName() + ": " + comment, report.getId(), null, null);
        }

        return toResponse(report);
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void require(String value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
