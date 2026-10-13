package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.AccountStatus;
import lk.tideline.cleanup.model.Role;
import lk.tideline.cleanup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNic(String nic);

    List<User> findByAvailableForAlertsTrue();

    List<User> findByRole(Role role);

    List<User> findAllByOrderByCreatedAtDescIdDesc();

    List<User> findByAccountStatusAndRoleInOrderByCreatedAtAscIdAsc(AccountStatus accountStatus, List<Role> roles);

    long countByRoleIn(List<Role> roles);

    /* An account says which province it is in, so that is as close as a district filter can get. */

    @Query("""
            select count(u) from User u
            where u.role in :roles and (:province is null or lower(u.province) = lower(:province))
            """)
    long countInProvinceByRoleIn(@Param("roles") List<Role> roles, @Param("province") String province);

    @Query("""
            select count(u) from User u
            where u.accountStatus = :status and (:province is null or lower(u.province) = lower(:province))
            """)
    long countInProvinceByAccountStatus(@Param("status") AccountStatus status,
                                        @Param("province") String province);

    long countByAccountStatus(AccountStatus accountStatus);
}
