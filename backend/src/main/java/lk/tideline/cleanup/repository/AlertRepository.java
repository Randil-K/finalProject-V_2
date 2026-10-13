package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.Alert;
import lk.tideline.cleanup.model.AlertReply;
import lk.tideline.cleanup.model.AlertType;
import lk.tideline.cleanup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByRecipientOrderByCreatedAtDescIdDesc(User recipient);

    long countByRecipientAndReadFlagFalse(User recipient);

    /** Who has already been asked to help with a project, so a wider call does not ask them twice. */
    List<Alert> findByProjectIdAndType(Long projectId, AlertType type);

    long countByType(AlertType type);

    /* Calls for help belong to the district of the cleanup they were sent about. */

    @Query("""
            select count(a) from Alert a
            where a.type = :type and (:district is null or exists (
                  select p.id from CleanupProject p left join p.report r
                  where p.id = a.projectId and r.district = :district))
            """)
    long countInDistrictByType(@Param("type") AlertType type, @Param("district") String district);

    @Query("""
            select count(a) from Alert a
            where a.type = :type and a.reply = :reply and (:district is null or exists (
                  select p.id from CleanupProject p left join p.report r
                  where p.id = a.projectId and r.district = :district))
            """)
    long countInDistrictByTypeAndReply(@Param("type") AlertType type, @Param("reply") AlertReply reply,
                                       @Param("district") String district);

    long countByTypeAndReply(AlertType type, AlertReply reply);

    /** Application notices saved before they had their own type were stored as account reviews. */
    @Transactional
    @Modifying
    @Query("update Alert a set a.type = lk.tideline.cleanup.model.AlertType.ACCOUNT_APPLICATION "
            + "where a.type = lk.tideline.cleanup.model.AlertType.ACCOUNT_REVIEW and a.title like 'New % to verify'")
    int retypeApplicationNotices();
}
