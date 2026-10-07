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
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private Role role = Role.CITIZEN;

    private String province;
    private String city;

    private Double latitude;
    private Double longitude;

    /** Whether the user currently wants cleanup alerts. */
    @Column(nullable = false)
    private boolean availableForAlerts = true;

    /** Set by an administrator for abusive accounts; a suspended user cannot sign in. */
    @Column(nullable = false)
    private boolean suspended = false;

    @Column(length = 500)
    private String suspensionReason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private AccountStatus accountStatus = AccountStatus.APPROVED;

    /** An administrator's reason when an application is not approved. */
    @Column(length = 500)
    private String accountReviewNote;

    private String organizationName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private OrganizationType organizationType;

    /** Generated file name of the profile picture under the uploads avatars folder, or null. */
    @Column(length = 80)
    private String avatarStoredName;

    /** Organisations give their website so an administrator can check they are genuine. */
    @Column(length = 300)
    private String websiteUrl;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private DiverProfile diverProfile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("uploadedAt ASC")
    private List<AccountDocument> documents = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
}
