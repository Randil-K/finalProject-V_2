package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.CleanupProject;
import lk.tideline.cleanup.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CleanupProjectRepository extends JpaRepository<CleanupProject, Long> {

    List<CleanupProject> findByStatusOrderByCreatedAtDesc(ProjectStatus status);

    List<CleanupProject> findAllByOrderByCreatedAtDesc();

    long countByStatus(ProjectStatus status);

    List<CleanupProject> findByReportIdOrderByCreatedAtDesc(Long reportId);

    Optional<CleanupProject> findFirstByReportId(Long reportId);

    boolean existsByReportId(Long reportId);

    List<CleanupProject> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /** Projects whose call for help is open: resources set, nothing gathered yet, never widened. */
    List<CleanupProject> findByResourcesFinalizedAtIsNotNullAndRecruitmentWidenedAtIsNullAndResourcesGatheredAtIsNull();
}
