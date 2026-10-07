package lk.tideline.cleanup.dto;

import lk.tideline.cleanup.model.Alert;
import lk.tideline.cleanup.model.AlertType;

import java.time.Instant;

public final class AlertDtos {

    private AlertDtos() {
    }

    public record AlertResponse(
            Long id,
            AlertType type,
            String title,
            String body,
            Long reportId,
            Long projectId,
            Double radiusKm,
            boolean read,
            boolean critical,
            Instant createdAt
    ) {
        public static AlertResponse from(Alert alert) {
            return new AlertResponse(
                    alert.getId(),
                    alert.getType(),
                    alert.getTitle(),
                    alert.getBody(),
                    alert.getReportId(),
                    alert.getProjectId(),
                    alert.getRadiusKm(),
                    alert.isReadFlag(),
                    alert.isCritical(),
                    alert.getCreatedAt());
        }
    }
}
