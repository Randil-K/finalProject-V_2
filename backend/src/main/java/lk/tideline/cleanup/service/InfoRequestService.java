package lk.tideline.cleanup.service;

import lk.tideline.cleanup.dto.InfoRequestDtos.InfoRequestResponse;
import lk.tideline.cleanup.model.*;
import lk.tideline.cleanup.repository.AlertRepository;
import lk.tideline.cleanup.repository.InfoRequestRepository;
import lk.tideline.cleanup.repository.PollutionReportRepository;
import lk.tideline.cleanup.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Module 4 — reviewers ask the reporter for more information. The request reaches the reporter as a
 * critical alert; their answer (description and photos) is visible only to reviewers and the reporter.
 */
@Service
public class InfoRequestService {

    private final InfoRequestRepository requests;
    private final PollutionReportRepository reports;
    private final UserRepository users;
    private final AlertRepository alertRepository;
    private final AlertService alerts;
    private final DocumentStorageService storage;

    public InfoRequestService(InfoRequestRepository requests, PollutionReportRepository reports, UserRepository users,
                              AlertRepository alertRepository, AlertService alerts, DocumentStorageService storage) {
        this.requests = requests;
        this.reports = reports;
        this.users = users;
        this.alertRepository = alertRepository;
        this.alerts = alerts;
        this.storage = storage;
    }

    /** Called from the review flow; the report's own decision fields are set by the caller. */
    @Transactional
    public InfoRequest open(PollutionReport report, User reviewer, String message) {
        if (requests.existsByReportAndStatus(report, InfoRequestStatus.OPEN)) {
            throw new IllegalStateException("The reporter hasn't answered the last request yet.");
        }
        InfoRequest request = new InfoRequest();
        request.setReport(report);
        request.setRequestedBy(reviewer);
        request.setMessage(message);
        requests.save(request);

        String who = reviewer.getRole() == Role.AUTHORITY ? "The government officer" : "An administrator";
        alerts.sendCritical(report.getReporter(), AlertType.INFO_REQUESTED,
                "Action needed: more information on " + report.getReference(),
                who + " asked: " + message,
                report.getId());
        return request;
    }

    @Transactional(readOnly = true)
    public InfoRequestStatus latestStatus(PollutionReport report) {
        return requests.findFirstByReportOrderByCreatedAtDescIdDesc(report).map(InfoRequest::getStatus).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<InfoRequestResponse> list(Long reportId, User viewer) {
        PollutionReport report = report(reportId);
        requireAccess(report, viewer);
        return requests.findByReportOrderByCreatedAtDescIdDesc(report).stream().map(InfoRequestResponse::from).toList();
    }

    @Transactional
    public InfoRequestResponse respond(Long reportId, Long requestId, User reporter, String description,
                                       List<MultipartFile> photos) {
        PollutionReport report = report(reportId);
        if (!report.getReporter().getId().equals(reporter.getId())) {
            throw new AccessDeniedException("Only the person who reported this can answer.");
        }
        InfoRequest request = requests.findById(requestId)
                .filter(found -> found.getReport().getId().equals(report.getId()))
                .orElseThrow(() -> new NotFoundException("That request was not found."));
        if (request.getStatus() != InfoRequestStatus.OPEN) {
            throw new IllegalStateException("You've already answered this request.");
        }

        List<DocumentStorageService.CheckedFile> files = storage.checkInfoPhotos(photos);
        request.setResponseText(description.trim());
        request.setStatus(InfoRequestStatus.ANSWERED);
        request.setRespondedAt(Instant.now());
        for (DocumentStorageService.CheckedFile file : files) {
            InfoAttachment attachment = new InfoAttachment();
            attachment.setRequest(request);
            attachment.setStoredName(storage.saveInfoPhoto(file));
            attachment.setOriginalName(file.originalName());
            attachment.setContentType(file.contentType());
            attachment.setSizeBytes(file.bytes().length);
            request.getAttachments().add(attachment);
        }
        requests.save(request);

        // The critical alert has done its job once it's answered.
        alertRepository.findByRecipientOrderByCreatedAtDescIdDesc(reporter).stream()
                .filter(alert -> alert.getType() == AlertType.INFO_REQUESTED && report.getId().equals(alert.getReportId()))
                .forEach(alert -> alert.setReadFlag(true));

        Map<Long, User> recipients = new LinkedHashMap<>();
        recipients.put(request.getRequestedBy().getId(), request.getRequestedBy());
        users.findByRole(Role.ADMIN).forEach(admin -> recipients.putIfAbsent(admin.getId(), admin));
        String body = reporter.getFullName() + " answered: \"" + preview(request.getMessage()) + "\""
                + (files.isEmpty() ? "" : " with " + files.size() + (files.size() == 1 ? " photo" : " photos"));
        recipients.values().forEach(recipient -> alerts.send(recipient, AlertType.INFO_RESPONSE,
                "More information received on " + report.getReference(), body, report.getId(), null, null));

        return InfoRequestResponse.from(request);
    }

    /** A photo from an answer, for reviewers and the reporter only. */
    @Transactional(readOnly = true)
    public AttachmentFile attachment(String storedName, User viewer) {
        InfoAttachment attachment = requests.findAttachment(storedName)
                .orElseThrow(() -> new NotFoundException("That file was not found."));
        requireAccess(attachment.getRequest().getReport(), viewer);
        return new AttachmentFile(storage.infoPhotoPath(storedName), attachment.getContentType());
    }

    public record AttachmentFile(Path path, String contentType) {
    }

    private void requireAccess(PollutionReport report, User viewer) {
        boolean reviewer = viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.AUTHORITY;
        if (!reviewer && !report.getReporter().getId().equals(viewer.getId())) {
            throw new AccessDeniedException("Only reviewers and the reporter can see this.");
        }
    }

    private PollutionReport report(Long id) {
        return reports.findById(id).orElseThrow(() -> new NotFoundException("Report " + id + " was not found."));
    }

    private static String preview(String text) {
        return text.length() > 80 ? text.substring(0, 77) + "..." : text;
    }
}
