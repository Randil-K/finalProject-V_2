package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.ReportDtos.CommentRequest;
import lk.tideline.cleanup.dto.ReportDtos.CommentResponse;
import lk.tideline.cleanup.dto.ReportDtos.CreateReportRequest;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.AlertRepository;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.NotFoundException;
import lk.tideline.cleanup.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Module 3 — replies and reactions in a report's discussion. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class DiscussionTests {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository users;

    @Autowired
    private AlertRepository alerts;

    @Test
    void repliesStayOneLevelDeepAndAlertTheCommentAuthor() {
        User reporter = user();
        User neighbour = user();
        User diver = user();
        Long reportId = report(reporter);

        CommentResponse question = reportService.comment(reportId, neighbour, new CommentRequest("Is it still there?", null));
        CommentResponse reply = reportService.comment(reportId, reporter, new CommentRequest("Yes, this morning.", question.id()));
        CommentResponse replyToReply = reportService.comment(reportId, diver, new CommentRequest("I can dive on Saturday.", reply.id()));

        assertThat(reply.parentId()).isEqualTo(question.id());
        assertThat(replyToReply.parentId()).isEqualTo(question.id());
        assertThat(alerts.findByRecipientOrderByCreatedAtDesc(neighbour))
                .extracting(Alert::getTitle)
                .contains(reporter.getFullName() + " replied to your comment");
        assertThat(alerts.findByRecipientOrderByCreatedAtDesc(reporter))
                .extracting(Alert::getType)
                .contains(AlertType.COMMENT_REPLY);
    }

    @Test
    void reactionsToggleAndSwitchLikeASocialPost() {
        User reporter = user();
        User viewer = user();
        Long reportId = report(reporter);
        Long commentId = reportService.comment(reportId, reporter, new CommentRequest("Bags of plastic.", null)).id();

        CommentResponse liked = reportService.react(reportId, commentId, ReactionType.LIKE, viewer);
        assertThat(liked.likeCount()).isEqualTo(1);
        assertThat(liked.myReaction()).isEqualTo(ReactionType.LIKE);

        CommentResponse hearted = reportService.react(reportId, commentId, ReactionType.HEART, viewer);
        assertThat(hearted.likeCount()).isZero();
        assertThat(hearted.heartCount()).isEqualTo(1);
        assertThat(hearted.myReaction()).isEqualTo(ReactionType.HEART);

        reportService.react(reportId, commentId, ReactionType.LIKE, reporter);
        List<CommentResponse> asViewer = reportService.comments(reportId, viewer);
        assertThat(asViewer.get(0).likeCount()).isEqualTo(1);
        assertThat(asViewer.get(0).heartCount()).isEqualTo(1);
        assertThat(asViewer.get(0).myReaction()).isEqualTo(ReactionType.HEART);

        CommentResponse removed = reportService.react(reportId, commentId, ReactionType.HEART, viewer);
        assertThat(removed.heartCount()).isZero();
        assertThat(removed.myReaction()).isNull();
    }

    @Test
    void aCommentFromAnotherReportCannotBeRepliedToOrReactedOn() {
        User reporter = user();
        Long first = report(reporter);
        Long second = report(reporter);
        Long commentId = reportService.comment(first, reporter, new CommentRequest("Here.", null)).id();

        assertThatThrownBy(() -> reportService.comment(second, reporter, new CommentRequest("Wrong thread.", commentId)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> reportService.react(second, commentId, ReactionType.LIKE, reporter))
                .isInstanceOf(NotFoundException.class);
    }

    private Long report(User reporter) {
        return reportService.create(new CreateReportRequest("Plastic on the shore", "Bottles.", Severity.LOW,
                "Galle", "Southern Province", 6.03, 80.21, null), reporter).id();
    }

    private User user() {
        User user = new User();
        user.setFullName("Tester " + UUID.randomUUID().toString().substring(0, 6));
        user.setEmail(UUID.randomUUID() + "@test.lk");
        user.setPasswordHash("x");
        user.setRole(Role.CITIZEN);
        return users.save(user);
    }
}
