package lk.tideline.cleanup.controller;

import lk.tideline.cleanup.dto.AnalyticsDtos.SummaryResponse;
import lk.tideline.cleanup.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public SummaryResponse summary(@RequestParam(required = false) String district) {
        return analyticsService.summary(district);
    }
}
