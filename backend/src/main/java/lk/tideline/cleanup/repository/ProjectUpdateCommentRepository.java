package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.ProjectUpdateComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectUpdateCommentRepository extends JpaRepository<ProjectUpdateComment, Long> {

    List<ProjectUpdateComment> findByUpdateIdInOrderByCreatedAtAscIdAsc(List<Long> updateIds);
}
