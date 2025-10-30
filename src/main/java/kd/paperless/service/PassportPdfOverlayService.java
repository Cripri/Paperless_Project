package kd.paperless.service;

import kd.paperless.dto.PassportForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Service
@RequiredArgsConstructor
public class PassportPdfOverlayService {

    // ✅ 실제 구현은 여기서 함
    private final PassportPdfService passportPdfService;

    /** 미리보기 PDF 생성 후 fileId 반환 */
    public String generatePreview(PassportForm form, Path photoPathIfAny) {
        try {
            // photoPathIfAny는 좌측 미리보기 이미지 용도로만 사용했다면 그대로 두고,
            // PDF 그리기는 form.getPhotoFile()을 PassportPdfService가 처리.
            return passportPdfService.makePreview(form);
        } catch (Exception e) {
            throw new RuntimeException("여권 미리보기 PDF 생성 실패", e);
        }
    }

    /** 프리뷰 파일 실제 경로 */
    public Path getPreviewPath(String fileId) {
        return passportPdfService.getPreviewPath(fileId);
    }

    /** 프리뷰 파일 크기 */
    public long sizeOfPreview(String fileId) {
        try {
            return Files.size(getPreviewPath(fileId));
        } catch (IOException e) {
            throw new RuntimeException("프리뷰 파일 사이즈 확인 실패: " + fileId, e);
        }
    }

    /** 프리뷰 파일 스트림 */
    public InputStream openPreviewStream(String fileId) {
        try {
            return Files.newInputStream(getPreviewPath(fileId), StandardOpenOption.READ);
        } catch (IOException e) {
            throw new RuntimeException("프리뷰 파일 스트림 열기 실패: " + fileId, e);
        }
    }
    
    /** 프리뷰 파일 삭제 */
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
            Path src = getPreviewPath(fileId);
            if (!Files.exists(src)) throw new IllegalArgumentException("미리보기 파일이 없습니다: " + fileId);

            // PassportPdfService와 동일 규칙으로 최종 경로 구성
            Path finalDir = Paths.get(System.getProperty("user.home"), "paperless", "rr_final");
            Files.createDirectories(finalDir);

            Path dest = finalDir.resolve(fileId + ".pdf");
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
            return dest;
        } catch (IOException e) {
            throw new RuntimeException("최종본 저장 실패", e);
        }
    }

    
}
