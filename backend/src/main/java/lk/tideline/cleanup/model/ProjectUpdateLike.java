package lk.tideline.cleanup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** One person liking one progress update. The unique key keeps a second like from counting twice. */
@Entity
@Table(
        name = "project_update_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"update_id", "user_id"})
)
@Getter
@Setter
public class ProjectUpdateLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "update_id")
    private ProjectUpdate update;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
