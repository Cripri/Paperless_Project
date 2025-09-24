package kd.paperless.service;

import kd.paperless.dto.ResidentRegistrationForm;
import kd.paperless.entity.PaperlessDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResidentRegistrationMapperService {

    public PaperlessDoc toEntity(ResidentRegistrationForm f) {
        PaperlessDoc e = new PaperlessDoc();
        // userId는 나중에 → null
        e.setUserId(101l);

        // 고정값
        e.setConsentYn('Y');
        e.setStatus(PaperlessDoc.PaperlessStatus.RECEIVED);
        e.setDocType("resident_registration");

        // 제출일자: 엔티티에 insertable=false면 DB default가 우선될 수 있음
        try {
            e.setSubmittedAt(LocalDateTime.now());
        } catch (Exception ignore) {}

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
        if (v == null) return;
        if (v instanceof String s && s.isBlank()) return;
        m.put(k, v);
    }
}
