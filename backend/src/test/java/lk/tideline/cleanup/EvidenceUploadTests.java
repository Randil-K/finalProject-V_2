package lk.tideline.cleanup;

import lk.tideline.cleanup.dto.ReportDtos.CreateReportRequest;
import lk.tideline.cleanup.dto.ReportDtos.EvidenceResponse;
import lk.tideline.cleanup.dto.ReportDtos.ReportResponse;
import lk.tideline.cleanup.model.Role;
import lk.tideline.cleanup.model.Severity;
import lk.tideline.cleanup.model.User;
import lk.tideline.cleanup.repository.UserRepository;
import lk.tideline.cleanup.service.DocumentStorageService;
import lk.tideline.cleanup.service.NotFoundException;
import lk.tideline.cleanup.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Module 2 — photo and video evidence uploaded with a report. */
@SpringBootTest
@TestPropertySource(properties = {"tideline.seed-demo-data=false", "tideline.uploads.directory=target/test-uploads"})
class EvidenceUploadTests {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] MP4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm', 0, 0, 0, 0};

    @Autowired
    private ReportService reportService;

    @Autowired
    private DocumentStorageService storage;

    @Autowired
    private UserRepository users;

    @Test
    void uploadedPhotosAndVideosAreStoredAndServedByType() throws Exception {
        ReportResponse report = reportService.create(request(),
                List.of(file("beach.png", PNG), file("tide.mp4", MP4)), reporter());

        assertThat(report.evidence()).extracting(EvidenceResponse::contentType)
                .containsExactly("image/png", "video/mp4");
        assertThat(report.photoUrls()).allMatch(url -> url.startsWith("/api/reports/evidence/"));

        String storedName = report.evidence().get(0).url().substring("/api/reports/evidence/".length());
        assertThat(Files.readAllBytes(storage.evidencePath(storedName))).isEqualTo(PNG);
    }

    @Test
    void aFileThatIsNotReallyAPhotoOrVideoIsRejected() {
        MultipartFile disguised = file("photo.jpg", "<html>not a photo</html>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> reportService.create(request(), List.of(disguised), reporter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a supported photo or video");
    }

    @Test
    void onlyGeneratedEvidenceNamesCanBeRead() {
        assertThatThrownBy(() -> storage.evidencePath("../certificate.pdf")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> storage.evidencePath(UUID.randomUUID() + ".pdf")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> storage.evidencePath(UUID.randomUUID() + ".jpg")).isInstanceOf(NotFoundException.class);
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("evidence", name, "application/octet-stream", bytes);
    }

    private static CreateReportRequest request() {
        return new CreateReportRequest("Plastic on the shore", "Bottles along the tide line.", Severity.MEDIUM,
                "Galle", "Southern Province", 6.03, 80.21, null);
    }

    private User reporter() {
        User user = new User();
        user.setFullName("Evidence tester");
        user.setEmail(UUID.randomUUID() + "@test.lk");
        user.setPasswordHash("x");
        user.setRole(Role.CITIZEN);
        return users.save(user);
    }
}
