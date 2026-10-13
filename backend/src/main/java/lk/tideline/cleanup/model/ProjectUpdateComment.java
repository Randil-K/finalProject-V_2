package lk.tideline.cleanup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** What someone said about a progress update on a cleanup. */
@Entity
@Table(name = "project_update_comments")
@Getter
@Setter
public class ProjectUpdateComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "update_id")
    private ProjectUpdate update;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
