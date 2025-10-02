package kd.paperless.service;

import kd.paperless.dto.ResidentRegistrationForm;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
public class RrPdfOverlayService {

    private static final Path PREVIEW_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "rr_preview");
    private static final Path FINAL_DIR   = Paths.get(System.getProperty("user.home"), "paperless", "rr_final");

    /** 미리보기 PDF 생성 후 fileId 반환 */
    public String makePreview(ResidentRegistrationForm f) throws Exception {
        Files.createDirectories(PREVIEW_DIR);
        String id  = UUID.randomUUID().toString().replace("-", "");
        Path   out = PREVIEW_DIR.resolve(id + ".pdf");

        try (InputStream tpl = getClass().getResourceAsStream("/pdf/rr_form.pdf")) {
            if (tpl == null) throw new FileNotFoundException("템플릿 PDF 누락: classpath:/pdf/rr_form.pdf");

            try (PDDocument doc = PDDocument.load(tpl)) {
                // 공통 폰트
                PDFont font = loadFontOrDefault(doc);

                // ---------- 페이지1: 기본정보 ----------
                PDPage page1 = doc.getPage(0);  

                // 페이지1 좌표
                Coords C1 = new Coords();
                C1.put("applicantName", 170, 670);
                C1.put("rrnFront",      400, 670);
                C1.put("rrnBack",       450, 670);
                C1.put("address1",      170, 620);
                C1.put("address2",      300, 620);
                C1.put("phone",         400, 530);
                C1.put("feeExempt_Y",          113, 493);

                try (PDPageContentStream cs =
                         new PDPageContentStream(doc, page1, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    drawText(cs, font, 11, C1.at("applicantName"), nvl(f.getApplicantName()));
                    drawText(cs, font, 11, C1.at("rrnFront"),      nvl(f.getRrnFront()));
                    drawText(cs, font, 11, C1.at("rrnBack"),       nvl(f.getRrnBack())); // or maskBack(f.getRrnBack())
                    drawText(cs, font, 11, C1.at("address1"),      nvl(f.getAddress1()));
                    drawText(cs, font, 11, C1.at("address2"),      nvl(f.getAddress2()));
                    drawText(cs, font, 11, C1.at("phone"),         nvl(f.getPhone()));

                    markIfYes(cs, font, C1, "feeExempt_Y", ynTrue(f.getFeeExempt()));
                    
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
                C2.put("residentregistration", 166, 750);
                C2.put("includeAll_ALL",       172, 692);
                C2.put("includeAll_PART",      200, 580);

                C2.put("addrHist_ALL",         305, 665);
                C2.put("addrHist_RECENT",      390, 665);
                C2.put("addrHist_RECENT_YRS",  493, 665);
                C2.put("addrHist_CUSTOM",      360, 552);

                C2.put("householdReason_Y",    504, 644);
                C2.put("householdDate_Y",      504, 623);
                C2.put("occurReport_Y",        504, 601);

                C2.put("changeReason_NONE",        397, 580);
                C2.put("changeReason_HOUSEHOLD",   493, 580);
                C2.put("changeReason_ALL_MEMBERS", 445, 580);

                C2.put("otherNames_Y",             504, 557);

                C2.put("rrnBack_NONE",             395, 536);
                C2.put("rrnBack_SELF",             443, 536);
                C2.put("rrnBack_HOUSEHOLD",        490, 536);

                C2.put("relToHead_Y",              504, 514);
                C2.put("cohabitants_Y",            504, 493);

                C2.put("signImage",                500, 20);
                C2.put("year",                     400,187);
                C2.put("month",                    468,187);
                C2.put("day,",                     520,187);

                try (PDPageContentStream cs2 =
                         new PDPageContentStream(doc, page2, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    
                    markDot(cs2, font, C2.at("residentregistration"));
                    
                    // 라디오
                    markRadio(cs2, font, C2, nvl(f.getIncludeAll()),
                              mapOf("ALL","includeAll_ALL", "PART","includeAll_PART"));

                    markRadio(cs2, font, C2, nvl(f.getAddressHistoryMode()),
                              mapOf("ALL","addrHist_ALL", "RECENT","addrHist_RECENT", "CUSTOM","addrHist_CUSTOM"));

                    if ("RECENT".equalsIgnoreCase(nvl(f.getAddressHistoryMode())) && f.getAddressHistoryYears()!=null) {
                        drawText(cs2, font, 11, C2.at("addrHist_RECENT_YRS"), f.getAddressHistoryYears() + "");
                    }   

                    // 체크(Y/N 또는 true/false)
                    markIfYes(cs2, font, C2, "householdReason_Y",   ynTrue(f.getIncludeHouseholdReason()));
                    markIfYes(cs2, font, C2, "householdDate_Y",     ynTrue(f.getIncludeHouseholdDate()));
                    markIfYes(cs2, font, C2, "occurReport_Y",       ynTrue(f.getIncludeOccurReportDates()));
                    markIfYes(cs2, font, C2, "otherNames_Y",        ynTrue(f.getIncludeOtherNames()));
                    markIfYes(cs2, font, C2, "relToHead_Y",         ynTrue(f.getIncludeRelationshipToHead()));
                    markIfYes(cs2, font, C2, "cohabitants_Y",       ynTrue(f.getIncludeCohabitants()));

                    // 라디오: 주민번호 뒷자리 포함 범위
                    markRadio(cs2, font, C2, nvl(f.getRrnBackInclusion()),
                              mapOf("NONE","rrnBack_NONE", "SELF","rrnBack_SELF", "HOUSEHOLD","rrnBack_HOUSEHOLD"));

                    

                    // 서명 이미지
                    if (isDataUrl(f.getSignatureBase64())) {
                        byte[] png = base64Data(f.getSignatureBase64());
                        PDImageXObject img = PDImageXObject.createFromByteArray(doc, png, "sign");
                        Pt p = C2.at("signImage");
                        float imgW = 50, imgH = 50;
                        cs2.drawImage(img, p.x, p.y, imgW, imgH);
                    }

                    Date today = new Date();
                    SimpleDateFormat toYear = new SimpleDateFormat("yyyy");
                    SimpleDateFormat toMonth = new SimpleDateFormat("MM");
                    SimpleDateFormat toDay = new SimpleDateFormat("dd");
                    String nowYear = toYear.format(today);
                    String nowMonth = toMonth.format(today);
                    String nowDay = toDay.format(today);

                    writeText(doc,page2,nowYear,font,400 ,187); // 연
                    writeText(doc,page2,nowMonth,font,468 ,187); // 월
                    writeText(doc,page2,nowDay,font,520 ,187); // 일
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

    private static String maskBack(String rrnBack) {
        if (rrnBack == null || rrnBack.length() != 7) return "";
        return rrnBack.charAt(0) + "******";
    }

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

    private void writeText(PDDocument doc, PDPage page, String str, PDFont font, int tx, int ty) throws IOException {
        try(PDPageContentStream con = new PDPageContentStream(doc,page,PDPageContentStream.AppendMode.APPEND,true,true)){
            con.beginText();
            con.setFont(font,14);
            con.newLineAtOffset(tx,ty);
            con.showText(str);
            con.endText();
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
