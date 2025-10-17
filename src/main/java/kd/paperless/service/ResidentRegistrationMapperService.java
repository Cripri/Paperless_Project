package kd.paperless.service;

import kd.paperless.dto.ResidentRegistrationForm;
import kd.paperless.entity.PaperlessDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResidentRegistrationMapperService {
    public PaperlessDoc toEntity(ResidentRegistrationForm f, Long loginId) {
        PaperlessDoc e = toEntity(f);
        e.setUserId(loginId);
        return e;
    }

    public PaperlessDoc toEntity(ResidentRegistrationForm f) {
        PaperlessDoc e = new PaperlessDoc();
        e.setUserId(null);

        // 고정값
        e.setConsentYn('Y');
        e.setStatus(PaperlessDoc.PaperlessStatus.RECEIVED);
        e.setDocType("resident_registration");

        // 제출일자: 엔티티에 insertable=false면 DB default가 우선될 수 있음
        try {
            e.setSubmittedAt(LocalDateTime.now());
        } catch (Exception ignore) {
        }

        // processed/admin: 비움
        e.setProcessedAt(null);
        e.setAdminId(null);

        // extraJson: 이름/주민번호 제외하고 나머지 저장
        Map<String, Object> extra = new LinkedHashMap<>();
        put(extra, "address1", f.getAddress1());
        put(extra, "address2", f.getAddress2());
        put(extra, "phone", f.getPhone());
        put(extra, "feeExempt", f.getFeeExempt());
        put(extra, "includeAll", f.getIncludeAll());
        put(extra, "addressHistoryMode", f.getAddressHistoryMode());
        put(extra, "addressHistoryYears", f.getAddressHistoryYears());
        put(extra, "includeHouseholdReason", f.getIncludeHouseholdReason());
        put(extra, "includeHouseholdDate", f.getIncludeHouseholdDate());
        put(extra, "includeOccurReportDates", f.getIncludeOccurReportDates());
        put(extra, "changeReasonScope", f.getChangeReasonScope());
        put(extra, "includeOtherNames", f.getIncludeOtherNames());
        put(extra, "rrnBackInclusion", f.getRrnBackInclusion());
        put(extra, "includeRelationshipToHead", f.getIncludeRelationshipToHead());
        put(extra, "includeCohabitants", f.getIncludeCohabitants());
        put(extra, "signatureBase64", f.getSignatureBase64());
        // docType/consentYn은 엔티티 고정 사용 → extraJson엔 굳이 안 넣음

        e.setExtraJson(extra);
        return e;
    }

    private void put(Map<String, Object> m, String k, Object v) {
        if (v == null)
            return;
        if (v instanceof String s && s.isBlank())
            return;
        m.put(k, v);
    }

    public ResidentRegistrationForm toForm(PaperlessDoc doc) {
        ResidentRegistrationForm f = ResidentRegistrationForm.defaultForGet();

        // 상단 메타
        f.setDocType("resident_registration");
        f.setConsentYn(String.valueOf(doc.getConsentYn()));

        Map<String, Object> m = doc.getExtraJson();
        if (m == null)
            m = new HashMap<>();
        final Map<String, Object> mapFinal = m; // 람다 캡처용

        java.util.function.Function<String, String> S = k -> mapToString(mapFinal, k);

        // ① 신청인 기본정보
        f.setApplicantName(S.apply("applicantName"));
        f.setRrnFront(S.apply("rrnFront"));
        f.setRrnBack(S.apply("rrnBack"));
        f.setAddress1(S.apply("address1"));
        f.setAddress2(S.apply("address2"));
        f.setPhone(S.apply("phone"));

        // ② 옵션
        f.setFeeExempt(S.apply("feeExempt")); // "Y"/"N"
        f.setIncludeAll(S.apply("includeAll")); // "ALL"/"PART"
        f.setAddressHistoryMode(S.apply("addressHistoryMode")); // "ALL"/"RECENT"/"CUSTOM"
        f.setAddressHistoryYears(toInt(mapFinal.get("addressHistoryYears")));
        f.setIncludeHouseholdReason(S.apply("includeHouseholdReason"));
        f.setIncludeHouseholdDate(S.apply("includeHouseholdDate"));
        f.setIncludeOccurReportDates(S.apply("includeOccurReportDates"));
        f.setChangeReasonScope(S.apply("changeReasonScope")); // "NONE"/"HOUSEHOLD"/"ALL_MEMBERS"
        f.setIncludeOtherNames(S.apply("includeOtherNames"));
        f.setRrnBackInclusion(S.apply("rrnBackInclusion")); // "NONE"/"SELF"/"HOUSEHOLD"
        f.setIncludeRelationshipToHead(S.apply("includeRelationshipToHead"));
        f.setIncludeCohabitants(S.apply("includeCohabitants"));

        // ③ 서명/기타
        f.setSignatureBase64(S.apply("signatureBase64"));

        // 필요시 원본 JSON 문자열 필드 사용 중이면
        f.setExtraJson(S.apply("extraJson"));

        // 기본값 보정
        if (f.getIncludeAll() == null)
            f.setIncludeAll("ALL");
        if (f.getFeeExempt() == null)
            f.setFeeExempt("N");

        return f;
    }

    private static String mapToString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return (v == null) ? null : String.valueOf(v);
    }

    private static Integer toInt(Object v) {
        if (v == null)
            return null;
        if (v instanceof Number n)
            return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
}
