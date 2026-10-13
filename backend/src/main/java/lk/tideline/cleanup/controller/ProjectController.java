package lk.tideline.cleanup.controller;

import jakarta.validation.Valid;
import lk.tideline.cleanup.dto.ProjectDtos.*;
import lk.tideline.cleanup.model.ProjectStatus;
import lk.tideline.cleanup.service.CurrentUserService;
import lk.tideline.cleanup.service.ProjectService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserService currentUser;

    public ProjectController(ProjectService projectService, CurrentUserService currentUser) {
        this.projectService = projectService;
        this.currentUser = currentUser;
    }

    /** Public, but a signed-in viewer also learns which cleanups they have joined. */
    @GetMapping
    public List<ProjectResponse> list(@RequestParam(required = false) ProjectStatus status,
                                      @RequestParam(required = false) Long reportId) {
        return projectService.list(status, reportId, currentUser.find().orElse(null));
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id) {
        return projectService.view(id, currentUser.find().orElse(null));
    }

    @PostMapping("/{id}/participants")
    public ProjectResponse join(@PathVariable Long id, @RequestBody(required = false) JoinProjectRequest request) {
        return projectService.join(id, currentUser.require(), request == null ? null : request.participantRole());
    }

    /** Module 8 — the organiser rates each participant once the cleanup is complete. */
    @PostMapping("/{id}/participants/{participantId}/mark")
    public ProjectResponse mark(@PathVariable Long id, @PathVariable Long participantId,
                                @Valid @RequestBody MarkRequest request) {
        return projectService.mark(id, participantId, request.mark(), currentUser.require());
    }

    /** The government officer revises the volunteers, divers and equipment they assigned on approval. */
    @PutMapping("/{id}/resources")
    @PreAuthorize("hasRole('AUTHORITY')")
    public ProjectResponse updateResources(@PathVariable Long id, @Valid @RequestBody ResourcesRequest request) {
        return projectService.updateResources(id, request, currentUser.require());
    }

    /** Module 7 — before / during / after progress evidence. */
    /** Multipart so the owner can show a photo: a JSON "data" part plus an optional "photo" file. */
    @PostMapping(value = "/{id}/updates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProjectResponse addUpdate(@PathVariable Long id,
                                     @Valid @RequestPart("data") ProjectUpdateRequest request,
                                     @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return projectService.addUpdate(id, request, photo, currentUser.require());
    }

    /** Module 7 — anyone signed in can like a progress update, or take the like back. */
    @PostMapping("/{id}/updates/{updateId}/likes")
    public ProjectResponse likeUpdate(@PathVariable Long id, @PathVariable Long updateId) {
        return projectService.likeUpdate(id, updateId, currentUser.require());
    }

    /** Module 7 — and say something about it. */
    @PostMapping("/{id}/updates/{updateId}/comments")
    public ProjectResponse commentOnUpdate(@PathVariable Long id, @PathVariable Long updateId,
                                           @Valid @RequestBody UpdateCommentRequest request) {
        return projectService.commentOnUpdate(id, updateId, request.body(), currentUser.require());
    }
}
