package kd.paperless.dto;

import java.io.Serializable;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ResidentRegistrationForm implements Serializable {
    // ① 신청인 기본정보
    @NotBlank(message = "성명을 입력하세요.")
    @Size(max = 100)
    private String applicantName;

    @Pattern(regexp = "^\\d{6}$", message = "주민등록번호 앞자리는 6자리 숫자입니다.")
    private String rrnFront;

    @Pattern(regexp = "^\\d{7}$", message = "주민등록번호 뒷자리는 7자리 숫자입니다.")
    private String rrnBack;

    @NotBlank(message = "주소를 입력하세요.")
    @Size(max = 200)
    private String address1;

    @Size(max = 200)
    private String address2;

    @Pattern(regexp = "^0\\d{1,2}-\\d{3,4}-\\d{4}$",
             message = "연락처 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phone;

    @Pattern(regexp = "^(Y|N)?$", message = "수수료 면제 여부는 Y/N 이어야 합니다.")
    private String feeExempt;
    
    /** 등본사항 전부 포함 여부: "ALL" 또는 "PART" (라디오) */
    @Pattern(regexp = "^(ALL|PART)$", message = "전부/부분 선택값이 올바르지 않습니다.")
    private String includeAll;

    /** 주소 변동사항 포함 모드: "ALL" | "RECENT" | "CUSTOM" (라디오) */
    @Pattern(regexp = "^(ALL|RECENT|CUSTOM)?$", message = "주소 변동사항 모드가 올바르지 않습니다.")
    private String addressHistoryMode;

    /** 주소 변동사항 '최근 N년'의 N 값(1~99) */
    @Min(value = 1, message = "최근 N년은 1 이상이어야 합니다.")
    @Max(value = 99, message = "최근 N년은 99 이하이어야 합니다.")
    private Integer addressHistoryYears;

    /** 세대 구성사유 포함 (체크) — "Y" / "N" */
    @Pattern(regexp = "^(Y|N)?$", message = "세대 구성사유 포함은 Y/N 이어야 합니다.")
    private String includeHouseholdReason;

    /** 세대 구성일자 포함 (체크) — "Y" / "N" */
    @Pattern(regexp = "^(Y|N)?$", message = "세대 구성일자 포함은 Y/N 이어야 합니다.")
    private String includeHouseholdDate;

    /** 발생일/신고일 포함 (체크) — "Y" / "N" */
    @Pattern(regexp = "^(Y|N)?$", message = "발생일/신고일 포함은 Y/N 이어야 합니다.")
    private String includeOccurReportDates;

    /** 변동사유 범위: "NONE" | "HOUSEHOLD" | "ALL_MEMBERS" (라디오) */
    @Pattern(regexp = "^(NONE|HOUSEHOLD|ALL_MEMBERS)?$", message = "변동사유 범위가 올바르지 않습니다.")
    private String changeReasonScope;

    /** 교부대상자 외 이름 포함 (체크) — "Y" / "N" */
    @Pattern(regexp = "^(Y|N)?$", message = "교부대상자 외 이름 포함은 Y/N 이어야 합니다.")
    private String includeOtherNames;

    /** 주민등록번호 뒷자리 포함 범위: "NONE" | "SELF" | "HOUSEHOLD" (라디오) */
    @Pattern(regexp = "^(NONE|SELF|HOUSEHOLD)?$", message = "주민등록번호 뒷자리 포함 범위가 올바르지 않습니다.")
    private String rrnBackInclusion;

    /** 세대주와의 관계 포함 (체크) — "Y" / "N" */
    @Pattern(regexp = "^(Y|N)?$", message = "세대주와의 관계 포함은 Y/N 이어야 합니다.")
    private String includeRelationshipToHead;

    /** 동거인 포함 (체크) — "Y" / "N" */
    @Pattern(regexp = "^(Y|N)?$", message = "동거인 포함은 Y/N 이어야 합니다.")
    private String includeCohabitants;

    // =========================
    // ③ 신청인 서명
    // =========================

    /** 서명 PNG(Base64 Data URL) */
    private String signatureBase64;

    // =========================
    // 히든(메타) 필드
    // =========================

    /** 문서 유형 (예: "resident_registration") */
    @Size(max = 50)
    private String docType;

    /** 개인정보 동의 여부 "Y"/"N" */
    @Pattern(regexp = "^(Y|N)?$", message = "개인정보 동의값은 Y/N 이어야 합니다.")
    private String consentYn;

    /** 확장 JSON(선택 필드들을 JSON으로 직렬화해 저장할 때 사용) */
    private String extraJson;

    // =========================
    // 편의 메서드
    // =========================

    /** GET 렌더링 시 기본값을 세팅한 인스턴스 */
    public static ResidentRegistrationForm defaultForGet() {
        ResidentRegistrationForm f = new ResidentRegistrationForm();
        f.setIncludeAll("ALL");     // 기본: 전부 포함
        f.setConsentYn("N");        // 기본: 미동의
        f.setDocType("resident_registration");
        // 체크박스/라디오는 미선택(null) 또는 "N"으로 초기화 가능
        return f;
    }

    /** 보안을 위해 rrnBack을 마스킹한 값을 돌려줌 (예: 1******) */
    public String getMaskedRrnBack() {
        if (rrnBack == null || rrnBack.length() != 7) return null;
        return rrnBack.charAt(0) + "******";
    }
}
