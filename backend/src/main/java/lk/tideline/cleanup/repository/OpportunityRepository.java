package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.Opportunity;
import lk.tideline.cleanup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    List<Opportunity> findByOpenTrueOrderByCreatedAtDescIdDesc();

    List<Opportunity> findByOrganizationOrderByCreatedAtDescIdDesc(User organization);
}
