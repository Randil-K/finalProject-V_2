package lk.tideline.cleanup.controller;

import jakarta.validation.Valid;
import lk.tideline.cleanup.dto.ReportDtos.*;
import lk.tideline.cleanup.model.ReportStatus;
import lk.tideline.cleanup.model.Severity;
import lk.tideline.cleanup.service.CurrentUserService;
import lk.tideline.cleanup.service.DocumentStorageService;
import lk.tideline.cleanup.service.InfoRequestService;
import lk.tideline.cleanup.dto.InfoRequestDtos.InfoRequestResponse;
import lk.tideline.cleanup.dto.InfoRequestDtos.InfoResponseRequest;
import lk.tideline.cleanup.service.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserService currentUser;
    private final DocumentStorageService storage;
    private final InfoRequestService infoRequests;

    public ReportController(ReportService reportService, CurrentUserService currentUser, DocumentStorageService storage,
                            InfoRequestService infoRequests) {
        this.reportService = reportService;
        this.currentUser = currentUser;
        this.storage = storage;
        this.infoRequests = infoRequests;
    }

    /** Requests for more information and the reporter's answers — reviewers and the reporter only. */
    @GetMapping("/{id}/info-requests")
    public List<InfoRequestResponse> infoRequests(@PathVariable Long id) {
        return infoRequests.list(id, currentUser.require());
    }

    @PostMapping(value = "/{id}/info-requests/{requestId}/response", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InfoRequestResponse respond(@PathVariable Long id, @PathVariable Long requestId,
                                       @Valid @RequestPart("data") InfoResponseRequest request,
                                       @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        return infoRequests.respond(id, requestId, currentUser.require(), request.description(), photos);
    }

    @GetMapping("/info-attachments/{fileName}")
    public ResponseEntity<Resource> infoAttachment(@PathVariable String fileName) {
        InfoRequestService.AttachmentFile file = infoRequests.attachment(fileName, currentUser.require());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file.path()));
    }

    @GetMapping
    public Page<ReportResponse> list(@RequestParam(required = false) ReportStatus status,
                                     @RequestParam(required = false) Severity severity,
                                     @RequestParam(required = false) String province,
                                     @RequestParam(defaultValue = "false") boolean reviewQueue,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        // reviewQueue=true lists only reports the community has verified: the administrators' queue.
        return reportService.search(status, severity, province, reviewQueue, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public ReportResponse get(@PathVariable Long id) {
        return reportService.view(id, currentUser.find().orElse(null));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportResponse> create(@Valid @RequestBody CreateReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.create(request, currentUser.require()));
    }

    /** Module 2: a report with uploaded photo and video evidence. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportResponse> createWithEvidence(@Valid @RequestPart("data") CreateReportRequest request,
                                                             @RequestPart(value = "evidence", required = false) List<MultipartFile> evidence) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.create(request, evidence, currentUser.require()));
    }

    /** Evidence is public like the reports themselves. Served as a resource so videos can seek. */
    @GetMapping("/evidence/{fileName}")
    public ResponseEntity<Resource> evidence(@PathVariable String fileName) {
        Path path = storage.evidencePath(fileName);
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }

    /** Module 3 — community verification vote. Sending the same choice again removes the vote. */
    @PostMapping("/{id}/votes")
    public ReportResponse vote(@PathVariable Long id, @Valid @RequestBody VoteRequest request) {
        return reportService.vote(id, currentUser.require(), request.confirmed());
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id) {
        return reportService.comments(id, currentUser.find().orElse(null));
    }

    @PostMapping("/{id}/comments/{commentId}/reactions")
    public CommentResponse react(@PathVariable Long id, @PathVariable Long commentId,
                                 @Valid @RequestBody ReactionRequest request) {
        return reportService.react(id, commentId, request.type(), currentUser.require());
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> comment(@PathVariable Long id,
                                                   @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.comment(id, currentUser.require(), request));
    }

    /** Module 4 — the administrator approves (sends to the authority), rejects, or requests more information. */
    @PostMapping("/{id}/moderation")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportResponse moderate(@PathVariable Long id, @Valid @RequestBody ModerationRequest request) {
        return reportService.moderate(id, request, currentUser.require());
    }

    /** Module 5 — the authority officer approves (creating the project), rejects, or requests more information. */
    @PostMapping("/{id}/authority-decision")
    @PreAuthorize("hasRole('AUTHORITY')")
    public ReportResponse decide(@PathVariable Long id, @Valid @RequestBody AuthorityDecisionRequest request) {
        return reportService.decideAsAuthority(id, request, currentUser.require());
    }
}
