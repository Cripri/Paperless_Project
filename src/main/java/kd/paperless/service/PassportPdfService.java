package kd.paperless.service;

import kd.paperless.dto.ResidentRegistrationForm;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PassportPdfService {

    private static final Path PREVIEW_DIR 
    = Paths.get(System.getProperty("java.io.tmpdir"), "rr_preview");
    private static final Path FINAL_DIR   
    = Paths.get(System.getProperty("user.home"), "paperless", "rr_final");

    /** 미리보기 PDF 생성 후 fileId 반환 */
    public String makePreview(ResidentRegistrationForm f) throws Exception {
        Files.createDirectories(PREVIEW_DIR);
        String id  = UUID.randomUUID().toString().replace("-", "");
        Path   out = PREVIEW_DIR.resolve(id + ".pdf");

        try (InputStream tpl = getClass().getResourceAsStream("/pdf/passport_form.pdf")) {
            if (tpl == null) throw new FileNotFoundException("템플릿 PDF 누락: classpath:/pdf/passport_form.pdf");

            try (PDDocument doc = PDDocument.load(tpl)) {
                // 공통 폰트
                PDFont font = loadFontOrDefault(doc);

                // ---------- 페이지1: 기본정보 ----------
                PDPage page1 = doc.getPage(0);  

                // 페이지1 좌표
                Coords C1 = new Coords();

                try (PDPageContentStream cs =
                         new PDPageContentStream(doc, page1, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    
                }

                // ---------- 페이지2: 라디오/체크/서명 ----------
                PDPage page2;
                if (doc.getNumberOfPages() >= 2) {
                    page2 = doc.getPage(1);
                } else {
                    page2 = new PDPage(page1.getMediaBox());
                    doc.addPage(page2);
                }

                // 페이지2 좌표 (레이아웃 다르면 여기서 조정)
                Coords C2 = new Coords();
                // 포함 범위 (라디오/체크)

                try (PDPageContentStream cs2 =
                         new PDPageContentStream(doc, page2, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    drawYmd(doc,page2,font,400,187);


                }

                // 저장
                doc.save(out.toFile());
            }
        }

        return id;
    }

    /** 미리보기 바이트 로드 */
    public byte[] loadBytes(String id) throws IOException {
        Path p = PREVIEW_DIR.resolve(id + ".pdf");
        return Files.exists(p) ? Files.readAllBytes(p) : null;
    }

    /** (호환) load → loadBytes 위임 */
    public byte[] load(String id) throws IOException {
        return loadBytes(id);
    }

    /** 프리뷰를 최종본으로 이동/복사 */
    public void promoteToFinal(String id) throws IOException {
        Files.createDirectories(FINAL_DIR);
        Path src = PREVIEW_DIR.resolve(id + ".pdf");
        if (Files.exists(src)) {
            Files.move(src, FINAL_DIR.resolve(id + ".pdf"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ========= 내부 유틸 =========

    private PDFont loadFontOrDefault(PDDocument doc) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fonts/NotoSansKR-VariableFont_wght.ttf")) {
            if (is != null) return PDType0Font.load(doc, is); // 2.x
        } catch (IOException ignore) {}
        return PDType1Font.HELVETICA; // 영문/숫자 대체
    }

    private static String nvl(Object o) { return (o == null) ? "" : String.valueOf(o); }


    // Y/N, "true"/"false", Boolean 모두 지원
    private static boolean ynTrue(String v) {
    if (v == null) return false;
    String s = v.trim();
    return "Y".equalsIgnoreCase(s)
        || "YES".equalsIgnoreCase(s)
        || "TRUE".equalsIgnoreCase(s)
        || "ON".equalsIgnoreCase(s)
        || "1".equals(s);
    }
    private static boolean ynTrue(Boolean v) { return Boolean.TRUE.equals(v); }
    private static boolean ynTrue(Object v) {
        if (v instanceof Boolean) return ynTrue((Boolean) v);
        if (v instanceof String)  return ynTrue((String) v);
        return false;
    }

    private static boolean isDataUrl(String s) { return s != null && s.startsWith("data:image"); }

    private static byte[] base64Data(String dataUrl) {
        int idx = dataUrl.indexOf(',');
        if (idx < 0) return new byte[0];
        String b64 = dataUrl.substring(idx + 1);
        return Base64.getDecoder().decode(b64);
    }

    private static void drawText(PDPageContentStream cs, PDFont font, int size, Pt p, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(p.x, p.y);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }

    private static void markDot(PDPageContentStream cs, PDFont font, Pt p) throws IOException {
        cs.beginText();
        cs.setFont(font, 13);
        cs.newLineAtOffset(p.x, p.y);
        cs.showText("●"); // 필요 시 "X"
        cs.endText();
    }

    private static void markIfYes(PDPageContentStream cs, PDFont font, Coords C, String key, boolean yes) throws IOException {
        if (yes) markDot(cs, font, C.at(key));
    }

    private static void markRadio(PDPageContentStream cs, PDFont font, Coords C,
                                  String value, Map<String, String> valueToKey) throws IOException {
        if (value == null) return;
        String key = valueToKey.get(value);
        if (key != null) markDot(cs, font, C.at(key));
    }

    private static Map<String,String> mapOf(String... kv) {
        Map<String,String> m = new HashMap<>();
        for (int i=0; i+1<kv.length; i+=2) m.put(kv[i], kv[i+1]);
        return m;
    }

    private static void writeText(PDDocument doc, PDPage page, String str, PDFont font, int tx, int ty) throws IOException {
        try(PDPageContentStream con = new PDPageContentStream(doc,page,PDPageContentStream.AppendMode.APPEND,true,true)){
            con.beginText();
            con.setFont(font,14);
            con.newLineAtOffset(tx,ty);
            con.showText(str);
            con.endText();
        }
    }

    private static void drawYmd(PDDocument doc, PDPage page, PDFont font, int posX, int posY) throws IOException{
        Date today = new Date();
        SimpleDateFormat toYear = new SimpleDateFormat("yyyy");
        SimpleDateFormat toMonth = new SimpleDateFormat("MM");
        SimpleDateFormat toDay = new SimpleDateFormat("dd");
        String nowYear = toYear.format(today);
        String nowMonth = toMonth.format(today);
        String nowDay = toDay.format(today);

        try(PDPageContentStream con = new PDPageContentStream(doc,page,PDPageContentStream.AppendMode.APPEND,true,true)){
            writeText(doc,page,nowYear,font,posX,posY);
            writeText(doc,page,nowMonth,font,posX+68,posY);
            writeText(doc,page,nowDay,font,posX+120,posY);
        }
    }

    // 좌표 도우미
    static class Coords {
        private final Map<String, Pt> map = new HashMap<>();
        void put(String k, float x, float y) { map.put(k, new Pt(x, y)); }
        Pt at(String k) {
            Pt p = map.get(k);
            if (p == null) throw new IllegalArgumentException("좌표 키 없음: " + k);
            return p;
        }
    }
    static class Pt {
        final float x, y;
        Pt(float x, float y) { this.x = x; this.y = y; }
    }
}
