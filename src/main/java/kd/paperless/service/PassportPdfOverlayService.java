package kd.paperless.service;

import kd.paperless.dto.PassportForm;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PassportPdfOverlayService {

    private static final Path PREVIEW_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "passport_preview");
    private static final Path FINAL_DIR   = Paths.get(System.getProperty("user.home"), "paperless", "passport_final");

    /** 미리보기 PDF 생성 후 fileId 반환 */
    public String generatePreview(PassportForm form, Path photoPathIfAny) {
        try {
            Files.createDirectories(PREVIEW_DIR);
            String fileId = UUID.randomUUID().toString().replace("-", "");
            Path out = PREVIEW_DIR.resolve(fileId + ".pdf");

            // TODO: 실제 템플릿 오버레이 구현 (PDFBox)
            try (PDDocument doc = new PDDocument()) {
                doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());
                doc.save(out.toFile());
            }
            return fileId;
        } catch (IOException e) {
            throw new RuntimeException("여권 미리보기 PDF 생성 실패", e);
        }
    }

    public Path getPreviewPath(String fileId) {
        return PREVIEW_DIR.resolve(fileId + ".pdf");
    }

    public long sizeOfPreview(String fileId) {
        try {
            return Files.size(getPreviewPath(fileId));
        } catch (IOException e) {
            throw new RuntimeException("프리뷰 파일 사이즈 확인 실패: " + fileId, e);
        }
    }

    public InputStream openPreviewStream(String fileId) {
        try {
            return Files.newInputStream(getPreviewPath(fileId));
        } catch (IOException e) {
            throw new RuntimeException("프리뷰 파일 스트림 열기 실패: " + fileId, e);
        }
    }

    public void cleanupPreview(String fileId) {
        try {
            Files.deleteIfExists(getPreviewPath(fileId));
        } catch (IOException e) {
            // 정책상 실패해도 진행
            System.err.println("cleanupPreview() failed: " + e.getMessage());
        }
    }

    /** (선택) 최종본 디렉토리 보관이 필요할 때 */
    public Path finalizeAndStore(String fileId) {
        try {
            Files.createDirectories(FINAL_DIR);
            Path src = getPreviewPath(fileId);
            if (!Files.exists(src)) throw new IllegalArgumentException("미리보기 파일이 없습니다: " + fileId);
            Path dest = FINAL_DIR.resolve(fileId + ".pdf");
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
            return dest;
        } catch (IOException e) {
            throw new RuntimeException("최종본 저장 실패", e);
        }
    }
}
