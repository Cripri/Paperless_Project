package kd.paperless.dto;

import lombok.Data;

@Data
public class ResidentRegistrationForm {
    // 기본정보
    private String applicantName;
    private String rrnFront;
    private String rrnBack;
    private String address;
    private String phone;
    private Boolean feeExempt;
    private String feeExemptReason;

    // 포함 범위
    private String includeAll; // "ALL" or "PART"

    // 주소변동
    private String addressHistoryMode;  // "ALL" / "RECENT" / "CUSTOM"
    private Integer addressHistoryYears;

    // 세대 관련
    private Boolean includeHouseholdReason;
    private Boolean includeHouseholdDate;

    // 발생/신고일
    private Boolean includeOccurReportDates;

    // 변동사유 범위
    private String changeReasonScope;   // "NONE" / "HOUSEHOLD" / "ALL_MEMBERS"

    // 기타 포함 항목
    private Boolean includeOtherNames;          // 교부대상자 외 이름
    private String rrnBackInclusion;            // "NONE"/"SELF"/"HOUSEHOLD"
    private Boolean includeRelationshipToHead;  // 세대원-세대주 관계
    private Boolean includeCohabitants;         // 동거인

    // 서명
    private String signatureBase64; // "data:image/png;base64,...."
}
