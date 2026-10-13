package lk.tideline.cleanup.repository;

import lk.tideline.cleanup.model.Opportunity;
import lk.tideline.cleanup.model.OpportunityApplication;
import lk.tideline.cleanup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OpportunityApplicationRepository extends JpaRepository<OpportunityApplication, Long> {

    Optional<OpportunityApplication> findByOpportunityAndDiver(Opportunity opportunity, User diver);

    List<OpportunityApplication> findByOpportunity(Opportunity opportunity);

    List<OpportunityApplication> findByDiverOrderByCreatedAtDescIdDesc(User diver);
}
