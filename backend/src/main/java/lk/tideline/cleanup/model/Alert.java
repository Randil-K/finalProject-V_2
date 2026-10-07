package lk.tideline.cleanup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "alerts")
@Getter
@Setter
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private AlertType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    private Long reportId;
    private Long projectId;

    /** Radius the alert was sent at, so escalation steps are auditable. */
    private Double radiusKm;

    @Column(nullable = false)
    private boolean readFlag = false;

    /** Needs the recipient to act, e.g. a reviewer asking the reporter for more information. */
    // Default so ddl-auto can add the column to a database that already has alerts.
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean critical = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
