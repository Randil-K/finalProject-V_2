package lk.tideline.cleanup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cleanup_projects")
@Getter
@Setter
public class CleanupProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-facing reference shown in the UI, e.g. CP-118. */
    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private PollutionReport report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.PLANNED;

    @Column(nullable = false)
    private int completionPercentage = 0;

    @Column(nullable = false)
    private String locationName;

    private String province;

    private Double latitude;
    private Double longitude;

    /** Recorded outcome of the cleanup, e.g. debris removed in kg. */
    private Double debrisRemovedKg;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectUpdate> updates = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectParticipant> participants = new ArrayList<>();

    /** Resources an administrator assigns after approval, based on the government officer's comment. */
    private Integer volunteersNeeded;

    private Integer diversNeeded;

    @ElementCollection
    @CollectionTable(name = "project_equipment", joinColumns = @JoinColumn(name = "project_id"))
    @OrderColumn(name = "position")
    private List<EquipmentItem> equipment = new ArrayList<>();

    /** Null while the resource plan is a draft only administrators and officers can see. */
    private Instant resourcesFinalizedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resources_finalized_by_id")
    private User resourcesFinalizedBy;

    private Instant startedAt;
    private Instant completedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
