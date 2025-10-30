package kd.paperless.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PassportPdfServiceDiag {

    private static final Path PREVIEW_DIR =
            Paths.get(System.getProperty("java.io.tmpdir"), "passport_preview");

    /** 템플릿 정보를 점검 (오류 없이 불러왔는지 확인용) */
    public Map<String, Object> pingTemplate() {
        Map<String, Object> info = new HashMap<>();
        info.put("classpath", "pdf/passport_form.pdf");
        try {
            Resource r = new ClassPathResource("pdf/passport_form.pdf");
            info.put("existsOnClasspath", r.exists());
            if (!r.exists()) return info;

            try (InputStream is = r.getInputStream(); PDDocument doc = PDDocument.load(is)) {
                int pages = doc.getNumberOfPages();
                info.put("pages", pages);
                if (pages > 0) {
                    info.put("mediaBox_page0", doc.getPage(0).getMediaBox().toString()); // 보통 [0,0,595,842]
                }
                info.put("ok", true);
            }
        } catch (Exception e) {
            info.put("ok", false);
            info.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return info;
    }

    /** 템플릿 그대로 저장 (수정 없이 echo). 반환값: fileId */
    public String echoTemplate() throws Exception {
        Files.createDirectories(PREVIEW_DIR);
        String id = UUID.randomUUID().toString().replace("-", "");
        Path out = PREVIEW_DIR.resolve(id + ".pdf");

        try (InputStream tpl = openTemplate();
             PDDocument doc = PDDocument.load(tpl)) {

            System.out.println("[DIAG] pages=" + doc.getNumberOfPages());
            PDPage p0 = doc.getNumberOfPages() > 0 ? doc.getPage(0) : null;
            System.out.println("[DIAG] mediaBox=" + (p0 != null ? p0.getMediaBox() : "null"));

            doc.save(out.toFile());
            System.out.println("[DIAG] saved echo: " + out.toAbsolutePath() +
                               " size=" + Files.size(out) + "B");
        }
        return id;
    }

    /** 페이지 전체를 검정색으로 채워서 저장 (그리기 정상 여부 확인). 반환값: fileId */
    public String debugFillBlack() throws Exception {
        Files.createDirectories(PREVIEW_DIR);
        String id = UUID.randomUUID().toString().replace("-", "");
        Path out = PREVIEW_DIR.resolve(id + ".pdf");

        try (InputStream tpl = openTemplate();
             PDDocument doc = PDDocument.load(tpl)) {

            PDPage page = doc.getPage(0);
            try (var cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(
                    doc, page, org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND, true, true)) {
                float w = page.getMediaBox().getWidth();
                float h = page.getMediaBox().getHeight();
                cs.addRect(0, 0, w, h);
                cs.setNonStrokingColor(0, 0, 0); // black
                cs.fill();
            }

            doc.save(out.toFile());
            System.out.println("[DIAG] saved black: " + out.toAbsolutePath() +
                               " size=" + Files.size(out) + "B");
        }
        return id;
    }

    /** 컨트롤러에서 바이트 제공 시 사용 (미리보기 응답의 Content-Type은 application/pdf로) */
    public byte[] loadBytes(String fileId) throws IOException {
        Path p = PREVIEW_DIR.resolve(fileId + ".pdf");
        return Files.exists(p) ? Files.readAllBytes(p) : null;
    }

    // ===== 내부 유틸 =====

    private InputStream openTemplate() throws IOException {
        Resource r = new ClassPathResource("pdf/passport_form.pdf");
        if (r.exists()) {
            System.out.println("[DIAG] classpath template OK: " + r);
            return r.getInputStream();
        }
        throw new IOException("classpath:/pdf/passport_form.pdf not found.");
    }
    public static Path getPreviewPath(String fileId) {
    return Paths.get(System.getProperty("java.io.tmpdir"), "rr_preview")
                .resolve(fileId + ".pdf");
    }

}
