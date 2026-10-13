package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.PollutionReport;
import lk.tideline.cleanup.model.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {

    List<ReportComment> findByReportOrderByCreatedAtAscIdAsc(PollutionReport report);
}
