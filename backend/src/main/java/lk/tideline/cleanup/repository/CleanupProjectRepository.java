package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.CleanupProject;
import lk.tideline.cleanup.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CleanupProjectRepository extends JpaRepository<CleanupProject, Long> {

    List<CleanupProject> findByStatusOrderByCreatedAtDescIdDesc(ProjectStatus status);

    List<CleanupProject> findAllByOrderByCreatedAtDescIdDesc();

    long countByStatus(ProjectStatus status);

    /* A project belongs to the district of the report it came from. */

    @Query("""
            select count(p) from CleanupProject p left join p.report r
            where p.status = :status and (:district is null or r.district = :district)
            """)
    long countInDistrictByStatus(@Param("status") ProjectStatus status, @Param("district") String district);

    @Query("""
            select count(p) from CleanupProject p left join p.report r
            where p.resourcesFinalizedAt is null and p.status <> :status
              and (:district is null or r.district = :district)
            """)
    long countInDistrictAwaitingResources(@Param("status") ProjectStatus status,
                                          @Param("district") String district);

    @Query("""
            select count(p) from CleanupProject p left join p.report r
            where p.resourcesGatheredAt is not null and p.status <> :status
              and (:district is null or r.district = :district)
            """)
    long countInDistrictReadyToRun(@Param("status") ProjectStatus status, @Param("district") String district);

    @Query("""
            select coalesce(sum(p.debrisRemovedKg), 0) from CleanupProject p left join p.report r
            where (:district is null or r.district = :district)
            """)
    double totalDebrisRemovedKgInDistrict(@Param("district") String district);

    /** Approved but the government officer has not said what it needs yet. */
    long countByResourcesFinalizedAtIsNullAndStatusNot(ProjectStatus status);

    /** Everything it asked for has been pledged. */
    long countByResourcesGatheredAtIsNotNullAndStatusNot(ProjectStatus status);

    @Query("select coalesce(sum(p.debrisRemovedKg), 0) from CleanupProject p")
    double totalDebrisRemovedKg();

    List<CleanupProject> findByReportIdOrderByCreatedAtDescIdDesc(Long reportId);

    Optional<CleanupProject> findFirstByReportId(Long reportId);

    boolean existsByReportId(Long reportId);

    List<CleanupProject> findByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    /** Resources set but not yet gathered — worth re-checking in case enough people have joined. */
    List<CleanupProject> findByResourcesFinalizedAtIsNotNullAndResourcesGatheredAtIsNull();

    /** Projects whose call for help is open: resources set, nothing gathered yet, never widened. */
    List<CleanupProject> findByResourcesFinalizedAtIsNotNullAndRecruitmentWidenedAtIsNullAndResourcesGatheredAtIsNull();
}
