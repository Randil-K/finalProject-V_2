package lk.tideline.cleanup.service;

import lk.tideline.cleanup.config.TidelineProperties;
import lk.tideline.cleanup.model.CleanupProject;
import lk.tideline.cleanup.model.ProjectStatus;
import lk.tideline.cleanup.repository.CleanupProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Module 6 — a call for help that has waited long enough without filling goes out again, further
 * afield. People already asked are skipped, so nobody hears about the same cleanup twice.
 */
@Component
public class RecruitmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentScheduler.class);

    private final CleanupProjectRepository projects;
    private final ProjectService projectService;
    private final TidelineProperties properties;

    public RecruitmentScheduler(CleanupProjectRepository projects, ProjectService projectService,
                                TidelineProperties properties) {
        this.projects = projects;
        this.projectService = projectService;
        this.properties = properties;
    }

    /**
     * A project can reach the people it needs without a fresh join — someone leaves, a target is
     * lowered, or the rule itself changes — so its state is checked again here rather than only
     * at the moment somebody signs up.
     */
    @Scheduled(fixedDelayString = "#{${tideline.alerts.widen-scan-minutes:15} * 60000}", initialDelay = 15000)
    @Transactional
    public void markProjectsThatAlreadyHaveTheirPeople() {
        for (CleanupProject project : projects.findByResourcesFinalizedAtIsNotNullAndResourcesGatheredAtIsNull()) {
            if (project.getStatus() != ProjectStatus.COMPLETED && projectService.hasEverythingItNeeds(project)) {
                projectService.catchUpGathered(project.getId());
                log.info("{} already had the people it needs", project.getReference());
            }
        }
    }

    @Scheduled(fixedDelayString = "#{${tideline.alerts.widen-scan-minutes:15} * 60000}", initialDelay = 60000)
    @Transactional
    public void widenCallsThatHaveWaitedLongEnough() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(properties.getAlerts().getWidenAfterDays()));
        List<CleanupProject> waiting = projects
                .findByResourcesFinalizedAtIsNotNullAndRecruitmentWidenedAtIsNullAndResourcesGatheredAtIsNull();

        for (CleanupProject project : waiting) {
            if (project.getStatus() == ProjectStatus.COMPLETED
                    || project.getResourcesFinalizedAt().isAfter(cutoff)
                    || projectService.hasEverythingItNeeds(project)) {
                continue;
            }
            int reached = projectService.widenCallForHelp(project.getId());
            log.info("Widened the call for help on {} to {} km, reaching {} more people",
                    project.getReference(), properties.getAlerts().getWidenedRadiusKm(), reached);
        }
    }
}
