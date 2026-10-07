package lk.tideline.cleanup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "project_updates")
@Getter
@Setter
public class ProjectUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private CleanupProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private UpdateStage stage = UpdateStage.DURING;

    @Column(nullable = false, length = 1000)
    private String note;

    @Column(length = 1000)
    private String imageUrl;

    /** Completion recorded with this update; drives the project's progress bar. */
    private Integer completionPercentage;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
