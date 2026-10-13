package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.InfoAttachment;
import lk.tideline.cleanup.model.InfoRequest;
import lk.tideline.cleanup.model.InfoRequestStatus;
import lk.tideline.cleanup.model.PollutionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InfoRequestRepository extends JpaRepository<InfoRequest, Long> {

    List<InfoRequest> findByReportOrderByCreatedAtDescIdDesc(PollutionReport report);

    Optional<InfoRequest> findFirstByReportOrderByCreatedAtDescIdDesc(PollutionReport report);

    boolean existsByReportAndStatus(PollutionReport report, InfoRequestStatus status);

    @Query("select a from InfoAttachment a join fetch a.request r join fetch r.report where a.storedName = :storedName")
    Optional<InfoAttachment> findAttachment(@Param("storedName") String storedName);
}
