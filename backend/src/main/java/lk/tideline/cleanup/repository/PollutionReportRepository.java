package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.PollutionReport;
import lk.tideline.cleanup.model.ReportStatus;
import lk.tideline.cleanup.model.ReviewDecision;
import lk.tideline.cleanup.model.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PollutionReportRepository extends JpaRepository<PollutionReport, Long> {

    @Query("""
            select r from PollutionReport r
            where ((:status is null and r.status not in :hidden) or r.status = :status)
              and (:severity is null or r.severity = :severity)
              and (:province is null or lower(r.province) = lower(:province))
            """)
    Page<PollutionReport> search(@Param("status") ReportStatus status,
                                 @Param("hidden") Collection<ReportStatus> hidden,
                                 @Param("severity") Severity severity,
                                 @Param("province") String province,
                                 Pageable pageable);

    @Query("""
            select r from PollutionReport r
            where r.status in :statuses
              and (:severity is null or r.severity = :severity)
              and (:province is null or lower(r.province) = lower(:province))
            """)
    Page<PollutionReport> searchIn(@Param("statuses") Collection<ReportStatus> statuses,
                                   @Param("severity") Severity severity,
                                   @Param("province") String province,
                                   Pageable pageable);

    /**
     * The review queue's "all": what the reviewers still have in hand, plus approvals whose project
     * is still waiting for a government officer to assign its resources.
     */
    @Query("""
            select r from PollutionReport r
            where (r.status in :statuses or (r.status = :approved and exists (
                      select p.id from CleanupProject p where p.report = r and p.resourcesFinalizedAt is null)))
              and (:severity is null or r.severity = :severity)
              and (:province is null or lower(r.province) = lower(:province))
            """)
    Page<PollutionReport> searchQueue(@Param("statuses") Collection<ReportStatus> statuses,
                                      @Param("approved") ReportStatus approved,
                                      @Param("severity") Severity severity,
                                      @Param("province") String province,
                                      Pageable pageable);

    /** Approved by the government officer, but the cleanup has no people or equipment assigned yet. */
    @Query("""
            select r from PollutionReport r
            where r.status = :approved
              and exists (select p.id from CleanupProject p where p.report = r and p.resourcesFinalizedAt is null)
              and (:severity is null or r.severity = :severity)
              and (:province is null or lower(r.province) = lower(:province))
            """)
    Page<PollutionReport> searchAwaitingResources(@Param("approved") ReportStatus approved,
                                                  @Param("severity") Severity severity,
                                                  @Param("province") String province,
                                                  Pageable pageable);

    /* The dashboard's counts, each of which can be narrowed to one district. */

    @Query("""
            select count(r) from PollutionReport r
            where r.status = :status and (:district is null or r.district = :district)
            """)
    long countInDistrictByStatus(@Param("status") ReportStatus status, @Param("district") String district);

    @Query("""
            select count(r) from PollutionReport r
            where r.status in :statuses and (:district is null or r.district = :district)
            """)
    long countInDistrictByStatusIn(@Param("statuses") Collection<ReportStatus> statuses,
                                   @Param("district") String district);

    @Query("""
            select count(r) from PollutionReport r
            where r.status not in :statuses and (:district is null or r.district = :district)
            """)
    long countInDistrictByStatusNotIn(@Param("statuses") Collection<ReportStatus> statuses,
                                      @Param("district") String district);

    @Query("""
            select r.province, count(r) from PollutionReport r
            where (:district is null or r.district = :district)
            group by r.province order by count(r) desc
            """)
    List<Object[]> countGroupedByProvinceIn(@Param("district") String district);

    @Query("""
            select r.locationName, count(r) from PollutionReport r
            where (:district is null or r.district = :district)
            group by r.locationName order by count(r) desc
            """)
    List<Object[]> countGroupedByLocationIn(@Param("district") String district);

    /** Rejected reports, split by who turned them down. */
    @Query("""
            select count(r) from PollutionReport r
            where r.status = :rejected and r.adminDecision = :decision
              and (:district is null or r.district = :district)
            """)
    long countInDistrictRejectedByAdmin(@Param("rejected") ReportStatus rejected,
                                        @Param("decision") ReviewDecision decision,
                                        @Param("district") String district);

    @Query("""
            select count(r) from PollutionReport r
            where r.status = :rejected and r.authorityDecision = :decision
              and (:district is null or r.district = :district)
            """)
    long countInDistrictRejectedByAuthority(@Param("rejected") ReportStatus rejected,
                                            @Param("decision") ReviewDecision decision,
                                            @Param("district") String district);

    List<PollutionReport> findByStatus(ReportStatus status);

    long countByStatus(ReportStatus status);

    long countByReporterId(Long reporterId);

    long countByStatusNotIn(Collection<ReportStatus> statuses);

    long countByStatusIn(Collection<ReportStatus> statuses);

    @Query("select r.province, count(r) from PollutionReport r group by r.province order by count(r) desc")
    List<Object[]> countGroupedByProvince();

    @Query("select r.locationName, count(r) from PollutionReport r group by r.locationName order by count(r) desc")
    List<Object[]> countGroupedByLocation();
}
