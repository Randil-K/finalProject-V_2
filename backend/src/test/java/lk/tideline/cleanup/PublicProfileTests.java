package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.ReportDtos.CreateReportRequest;
import lk.tideline.cleanup.dto.UserDtos.PublicProfileResponse;
import lk.tideline.cleanup.model.AccountStatus;
import lk.tideline.cleanup.model.Role;
import lk.tideline.cleanup.model.Severity;
import lk.tideline.cleanup.model.User;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.NotFoundException;
import lk.tideline.cleanup.service.ReportService;
import lk.tideline.cleanup.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Profiles other members can open. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class PublicProfileTests {

    @Autowired
    private UserService userService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository users;

    @Test
    void aPublicProfileShowsActivityButNoContactDetails() {
        User member = user(Role.CITIZEN, AccountStatus.APPROVED);
        reportService.create(new CreateReportRequest("Plastic", "Bottles.", Severity.LOW,
                "Galle", "Southern Province", 6.03, 80.21, null), member);

        PublicProfileResponse profile = userService.publicProfile(member.getId(), user(Role.CITIZEN, AccountStatus.APPROVED));

        assertThat(profile.fullName()).isEqualTo(member.getFullName());
        assertThat(profile.reportsSubmitted()).isEqualTo(1);
        assertThat(profile.verified()).isTrue();
        assertThat(Arrays.stream(PublicProfileResponse.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("email", "phone", "latitude", "longitude");
    }

    @Test
    void unverifiedAccountsAreHiddenFromOtherMembersButNotFromAdmins() {
        User pending = user(Role.DIVER, AccountStatus.PENDING_REVIEW);

        assertThatThrownBy(() -> userService.publicProfile(pending.getId(), user(Role.CITIZEN, AccountStatus.APPROVED)))
                .isInstanceOf(NotFoundException.class);
        assertThat(userService.publicProfile(pending.getId(), user(Role.ADMIN, AccountStatus.APPROVED)).verified()).isFalse();
        assertThat(userService.publicProfile(pending.getId(), pending)).isNotNull();
    }

    private User user(Role role, AccountStatus status) {
        User user = new User();
        user.setFullName("Member " + UUID.randomUUID().toString().substring(0, 6));
        user.setEmail(UUID.randomUUID() + "@test.lk");
        user.setPasswordHash("x");
        user.setRole(role);
        user.setAccountStatus(status);
        user.setPhone("0771234567");
        return users.save(user);
    }
}
