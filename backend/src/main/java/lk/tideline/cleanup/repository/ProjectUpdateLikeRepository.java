package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.ProjectUpdate;
import lk.tideline.cleanup.model.ProjectUpdateLike;
import lk.tideline.cleanup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectUpdateLikeRepository extends JpaRepository<ProjectUpdateLike, Long> {

    Optional<ProjectUpdateLike> findByUpdateAndUser(ProjectUpdate update, User user);

    long countByUpdate(ProjectUpdate update);

    List<ProjectUpdateLike> findByUpdateIdIn(List<Long> updateIds);
}
