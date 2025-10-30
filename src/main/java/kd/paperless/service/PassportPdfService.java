package kd.paperless.service;

import kd.paperless.dto.PassportForm;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PassportPdfService {

    private static final Path PREVIEW_DIR =
            Paths.get(System.getProperty("java.io.tmpdir"), "rr_preview");
    private static final Path FINAL_DIR =
            Paths.get(System.getProperty("user.home"), "paperless", "rr_final");

    /** 미리보기 PDF 생성 후 fileId 반환 */
    public String makePreview(PassportForm f) throws Exception {
        Files.createDirectories(PREVIEW_DIR);
        String id  = UUID.randomUUID().toString().replace("-", "");
        Path   out = PREVIEW_DIR.resolve(id + ".pdf");

        try (InputStream tpl = openTemplate();
             PDDocument doc = PDDocument.load(tpl)) {

            // 폰트
            PDFont font = loadFontOrDefault(doc);

            // ---------- 페이지1 ----------
            PDPage page1 = doc.getPage(0);

            // 좌표 맵
            Coords C1 = new Coords();
            // ① 기본 선택
            C1.put("passportType_NORMAL",      176, 723);
            C1.put("passportType_OFFICIAL",    160, 723);
            C1.put("passportType_DIPLOMAT",    146, 723);
            C1.put("passportType_EMERGENCY",   128, 723);
            C1.put("passportType_TRAVEL_CERT", 114, 723);

            C1.put("travelMode_ROUND",  89, 723);
            C1.put("travelMode_ONEWAY", 76, 723);

            C1.put("pageCount_26", 32, 723);
            C1.put("pageCount_58", 21, 723);

            C1.put("validity_10Y",       176, 702);
            C1.put("validity_1Y_SINGLE", 160, 702);
            C1.put("validity_REMAINING", 136, 702);

            // ② 인적 사항
            C1.put("koreanName",        137, 656);
            C1.put("rrnFront",          138, 630);
            C1.put("rrnBack",            77, 630);
            C1.put("phone",             138, 607);

            C1.put("emergencyName",     138, 569);
            C1.put("emergencyRelation",  46, 569);
            C1.put("emergencyPhone",    138, 541);

            C1.put("engLastName",       177, 490);
            C1.put("engFirstName",      177, 465);
            C1.put("spouseEngLastName", 177, 395);

            C1.put("braillePassport_Y", 176, 372);
            C1.put("braillePassport_N", 157, 372);

            // ③ 우편 배송
            C1.put("deliveryWanted_Y", 196, 340);
            C1.put("deliveryWanted_N", 197, 340);
            C1.put("deliveryPostcode",   0,   0); // 좌표 미정이면 0,0 → 출력 생략 권장
            C1.put("deliveryAddress1",  92, 350);
            C1.put("deliveryAddress2",  92, 335);

            // ④ 사진(좌하단 앵커)
            C1.put("photo_anchor", 220, 529);

            // 신청일자(yyyy/MM/dd 각각) — 0,0이면 출력 생략
            C1.put("applyYear",   0, 0);
            C1.put("applyMonth",  0, 0);
            C1.put("applyDay",    0, 0);

            try (PDPageContentStream cs =
                         new PDPageContentStream(doc, page1, PDPageContentStream.AppendMode.APPEND, true, true)) {

                // 라디오/체크
                markRadio(cs, font, C1, nvl(f.getPassportType()),
                        mapOf(
                                "NORMAL",      "passportType_NORMAL",
                                "OFFICIAL",    "passportType_OFFICIAL",
                                "DIPLOMAT",    "passportType_DIPLOMAT",
                                "EMERGENCY",   "passportType_EMERGENCY",
                                "TRAVEL_CERT", "passportType_TRAVEL_CERT"
                        ));

                if ("TRAVEL_CERT".equalsIgnoreCase(nvl(f.getPassportType()))) {
                    markRadio(cs, font, C1, nvl(f.getTravelMode()),
                            mapOf("ROUND","travelMode_ROUND", "ONEWAY","travelMode_ONEWAY"));
                }

                if (Objects.equals(f.getPageCount(), 26)) markDot(cs, font, C1.at("pageCount_26"));
                if (Objects.equals(f.getPageCount(), 58)) markDot(cs, font, C1.at("pageCount_58"));

                markRadio(cs, font, C1, nvl(f.getValidity()),
                        mapOf("10Y","validity_10Y","1Y_SINGLE","validity_1Y_SINGLE","REMAINING","validity_REMAINING"));

                markRadio(cs, font, C1, ynTrue(f.getBraillePassport()) ? "Y" : "N",
                        mapOf("Y","braillePassport_Y","N","braillePassport_N"));

                markRadio(cs, font, C1, ynTrue(f.getDeliveryWanted()) ? "Y" : "N",
                        mapOf("Y","deliveryWanted_Y","N","deliveryWanted_N"));

                // 텍스트 (글자별 -9pt)
                drawTextPerChar(cs, font, 11, C1.at("koreanName"),        nvl(f.getKoreanName()), -9f);
                drawTextPerChar(cs, font, 11, C1.at("rrnFront"),          nvl(f.getRrnFront()),   -9f);
                drawTextPerChar(cs, font, 11, C1.at("rrnBack"),           nvl(f.getRrnBack()),    -9f);
                drawTextPerChar(cs, font, 11, C1.at("phone"),             nvl(f.getPhone()),      -9f);

                drawTextPerChar(cs, font, 11, C1.at("emergencyName"),     nvl(f.getEmergencyName()),     -9f);
                drawTextPerChar(cs, font, 11, C1.at("emergencyRelation"), nvl(f.getEmergencyRelation()), -9f);
                drawTextPerChar(cs, font, 11, C1.at("emergencyPhone"),    nvl(f.getEmergencyPhone()),    -9f);

                drawTextPerChar(cs, font, 11, C1.at("engLastName"),       nvl(f.getEngLastName()),       -9f);
                drawTextPerChar(cs, font, 11, C1.at("engFirstName"),      nvl(f.getEngFirstName()),      -9f);
                drawTextPerChar(cs, font, 11, C1.at("spouseEngLastName"), nvl(f.getSpouseEngLastName()), -9f);

                // 우편 배송 텍스트 (우편번호 좌표가 0,0이면 생략)
                if (!atZero(C1.at("deliveryPostcode")))
                    drawTextPerChar(cs, font, 11, C1.at("deliveryPostcode"), nvl(f.getDeliveryPostcode()), -9f);
                drawTextPerChar(cs, font, 11, C1.at("deliveryAddress1"), nvl(f.getDeliveryAddress1()), -9f);
                drawTextPerChar(cs, font, 11, C1.at("deliveryAddress2"), nvl(f.getDeliveryAddress2()), -9f);

                // 사진
                MultipartFile photo = f.getPhotoFile();
                if (photo != null && !photo.isEmpty()) {
                    byte[] bytes = photo.getBytes();
                    PDImageXObject img = PDImageXObject.createFromByteArray(doc, bytes, "photo");
                    Pt p = C1.at("photo_anchor");
                    float w = 120f, h = 160f;
                    cs.drawImage(img, p.x, p.y, w, h);
                }

                // 신청일자 (좌표 설정되어 있을 때만)
                if (!atZero(C1.at("applyYear")))  drawText(cs, font, 11, C1.at("applyYear"),   new SimpleDateFormat("yyyy").format(new Date()));
                if (!atZero(C1.at("applyMonth"))) drawText(cs, font, 11, C1.at("applyMonth"), new SimpleDateFormat("MM").format(new Date()));
                if (!atZero(C1.at("applyDay")))   drawText(cs, font, 11, C1.at("applyDay"),   new SimpleDateFormat("dd").format(new Date()));
            }

            // 저장 + 로그
            doc.save(out.toFile());
            System.out.println("[PassportPDF] saved " + out.toAbsolutePath() + " size=" + Files.size(out) + "B");
        }

        return id;
    }

    /** 미리보기 바이트 로드 */
    public byte[] loadBytes(String id) throws IOException {
        Path p = PREVIEW_DIR.resolve(id + ".pdf");
        return Files.exists(p) ? Files.readAllBytes(p) : null;
    }

    /** (호환) load → loadBytes 위임 */
    public byte[] load(String id) throws IOException { return loadBytes(id); }

    /** 프리뷰를 최종본으로 이동/복사 */
    public void promoteToFinal(String id) throws IOException {
        Files.createDirectories(FINAL_DIR);
        Path src = PREVIEW_DIR.resolve(id + ".pdf");
        if (Files.exists(src)) {
            Files.move(src, FINAL_DIR.resolve(id + ".pdf"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ========= 내부 유틸 =========

    private InputStream openTemplate() throws IOException {
        Resource r = new ClassPathResource("pdf/passport_form.pdf");
        if (r.exists()) return r.getInputStream();
        throw new FileNotFoundException("템플릿 PDF 누락: classpath:/pdf/passport_form.pdf");
    }

    private PDFont loadFontOrDefault(PDDocument doc) throws IOException {
        try {
            Resource r = new ClassPathResource("fonts/NotoSansKR-VariableFont_wght.ttf");
            if (r.exists()) return PDType0Font.load(doc, r.getInputStream());
            System.out.println("[PassportPDF] NotoSansKR not found → Helvetica fallback (한글 미표시 가능)");
        } catch (IOException ignore) {}
        return PDType1Font.HELVETICA;
    }

    private static String nvl(Object o) { return (o == null) ? "" : String.valueOf(o); }

    private static boolean ynTrue(String v) {
        if (v == null) return false;
        String s = v.trim();
        return "Y".equalsIgnoreCase(s) || "YES".equalsIgnoreCase(s)
                || "TRUE".equalsIgnoreCase(s) || "ON".equalsIgnoreCase(s)
                || "1".equals(s);
    }
    private static boolean ynTrue(Boolean v) { return Boolean.TRUE.equals(v); }
    private static boolean ynTrue(Object v) {
        if (v instanceof Boolean) return ynTrue((Boolean) v);
        if (v instanceof String)  return ynTrue((String) v);
        return false;
    }

    private static boolean atZero(Pt p){ return p != null && p.x == 0f && p.y == 0f; }

    private static void drawText(PDPageContentStream cs, PDFont font, int size, Pt p, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(p.x, p.y);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }

    /** 텍스트를 한 글자씩 x를 stepX만큼 이동하며 찍기 (안전한 버전) */
    private static void drawTextPerChar(PDPageContentStream cs, PDFont font, int size,
                                        Pt start, String text, float stepX) throws IOException {
        if (text == null || text.isEmpty()) return;
        float x = start.x, y = start.y;
        cs.beginText();
        cs.setFont(font, size); // 반드시 텍스트 블록 안
        for (int i = 0; i < text.length(); i++) {
            cs.setTextMatrix(1, 0, 0, 1, x, y);
            cs.showText(text.substring(i, i + 1));
            x -= stepX; // -9이면 왼쪽으로 9pt씩
        }
        cs.endText();
    }

    private static void markDot(PDPageContentStream cs, PDFont font, Pt p) throws IOException {
        cs.beginText();
        cs.setFont(font, 13);
        cs.newLineAtOffset(p.x, p.y);
        cs.showText("●");
        cs.endText();
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

    // ===== 좌표 도우미 =====
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

    public static Path getPreviewPath(String fileId) {
        return Paths.get(System.getProperty("java.io.tmpdir"), "rr_preview");
    }



}
