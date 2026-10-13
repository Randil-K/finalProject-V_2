package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.CleanupProject;
import lk.tideline.cleanup.model.ParticipantRole;
import lk.tideline.cleanup.model.ProjectParticipant;
import lk.tideline.cleanup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant, Long> {

    Optional<ProjectParticipant> findByProjectAndUser(CleanupProject project, User user);

    List<ProjectParticipant> findByUser(User user);

    List<ProjectParticipant> findByProjectOrderByJoinedAtAscIdAsc(CleanupProject project);

    long countByProjectAndParticipantRole(CleanupProject project, ParticipantRole participantRole);

    /** Everyone who joined a cleanup, or only those in one district. */
    @Query("""
            select count(p) from ProjectParticipant p left join p.project pr left join pr.report r
            where (:district is null or r.district = :district)
            """)
    long countInDistrict(@Param("district") String district);

    /** How many people joined a cleanup in each province. */
    @Query("""
            select pr.province, count(p) from ProjectParticipant p
            join p.project pr left join pr.report r
            where (:district is null or r.district = :district)
            group by pr.province order by count(p) desc
            """)
    List<Object[]> countGroupedByProjectProvince(@Param("district") String district);

    /** Average organiser rating (1-5) across the cleanups this user took part in; null if none yet. */
    @Query("select avg(p.contributionMark) from ProjectParticipant p where p.user.id = :userId and p.contributionMark is not null")
    Double averageMark(@Param("userId") Long userId);

    long countByUserIdAndContributionMarkIsNotNull(Long userId);
}
