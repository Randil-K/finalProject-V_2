package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.ProjectUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectUpdateRepository extends JpaRepository<ProjectUpdate, Long> {
}
