package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.Alert;
import lk.tideline.cleanup.model.AlertType;
import lk.tideline.cleanup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByRecipientOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndReadFlagFalse(User recipient);

    /** Who has already been asked to help with a project, so a wider call does not ask them twice. */
    List<Alert> findByProjectIdAndType(Long projectId, AlertType type);

    /** Application notices saved before they had their own type were stored as account reviews. */
    @Transactional
    @Modifying
    @Query("update Alert a set a.type = lk.tideline.cleanup.model.AlertType.ACCOUNT_APPLICATION "
            + "where a.type = lk.tideline.cleanup.model.AlertType.ACCOUNT_REVIEW and a.title like 'New % to verify'")
    int retypeApplicationNotices();
}
