package lk.tideline.cleanup.dto;

import jakarta.validation.constraints.NotNull;
import lk.tideline.cleanup.model.Alert;
import lk.tideline.cleanup.model.AlertReply;
import lk.tideline.cleanup.model.AlertType;
import lk.tideline.cleanup.dto.ProjectDtos.EquipmentLine;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

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
            /** How the recipient answered a call for help; null while it is still open. */
            AlertReply reply,
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
                    alert.getReply(),
                    alert.getCreatedAt());
        }
    }

    /** Answering a call for help: joining the cleanup, with anything you can bring, or letting it pass. */
    public record AlertReplyRequest(
            @NotNull AlertReply reply,
            @Size(max = 30) List<@Valid EquipmentLine> equipment
    ) {
    }
}
