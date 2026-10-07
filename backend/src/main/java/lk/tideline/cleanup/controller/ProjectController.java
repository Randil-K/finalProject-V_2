package lk.tideline.cleanup.controller;

import jakarta.validation.Valid;
import lk.tideline.cleanup.dto.ProjectDtos.*;
import lk.tideline.cleanup.model.ProjectStatus;
import lk.tideline.cleanup.service.CurrentUserService;
import lk.tideline.cleanup.service.ProjectService;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PostMapping("/{id}/updates")
    public ProjectResponse addUpdate(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        return projectService.addUpdate(id, request, currentUser.require());
    }
}
