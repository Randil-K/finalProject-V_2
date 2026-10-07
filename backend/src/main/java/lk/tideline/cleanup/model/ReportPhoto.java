package lk.tideline.cleanup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "report_photos")
@Getter
@Setter
public class ReportPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id")
    private PollutionReport report;

    /** Where the photo or video is served from: /api/reports/evidence/{storedName} for uploads. */
    @Column(nullable = false, length = 1000)
    private String url;

    /** Set for uploaded files; null for evidence referenced by an external URL. */
    @Column(unique = true, length = 80)
    private String storedName;

    @Column(length = 100)
    private String contentType;

    private String caption;
}
