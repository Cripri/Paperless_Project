package kd.paperless.entity;

import jakarta.persistence.*;
import kd.paperless.config.converter.JsonMapConverter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * paperless_doc 테이블 매핑
 *  - PK: pl_id (시퀀스 SEQ_PAPERLESS_PL_ID)
 *  - 컬럼: user_id, consent_yn(Y/N), status, submitted_at, processed_at,
 *          admin_id, doc_type, extra_json(JSON CLOB)
 */
@Entity
@Table(name = "paperless_doc")
@SequenceGenerator(
        name = "paperless_pl_id_seq",
        sequenceName = "SEQ_PAPERLESS_PL_ID",
        allocationSize = 1
)
public class PaperlessDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paperless_pl_id_seq")
    @Column(name = "pl_id", nullable = false)
    private Long plId;

    /** users.user_id (숫자 PK 가정) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 'Y' / 'N' */
    @Column(name = "consent_yn", nullable = false, length = 1)
    private String consentYn = "N";

    /** 상태값: PENDING/RECEIVED/IN_PROGRESS/APPROVED/REJECTED/CANCELED/COMPLETED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaperlessStatus status = PaperlessStatus.PENDING;

    /** DB default SYSDATE 사용 → insert 시 DB가 넣도록 위임 */
    @Column(name = "submitted_at", insertable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** admin.admin_id FK 가정 */
    @Column(name = "admin_id")
    private Long adminId;

    /** 문서 종류 코드/명칭 (등본/초본/여권 등) */
    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType;

    /** 확장 JSON (스파스 저장 권장) */
    @Convert(converter = JsonMapConverter.class)
    @Lob
    @JdbcTypeCode(SqlTypes.CLOB) // Hibernate 6.x에서 CLOB 타입 명시
    @Column(name = "extra_json", nullable = false, columnDefinition = "CLOB")
    private Map<String, Object> extraJson = new HashMap<>();

    // ===== 기본 생성자 =====
    public PaperlessDoc() {}

    // ===== 편의 생성자 =====
    public PaperlessDoc(Long userId, String docType) {
        this.userId = userId;
        this.docType = docType;
    }

    // ===== Getter / Setter =====
    public Long getPlId() { return plId; }
    public void setPlId(Long plId) { this.plId = plId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getConsentYn() { return consentYn; }
    public void setConsentYn(String consentYn) { this.consentYn = consentYn; }

    public PaperlessStatus getStatus() { return status; }
    public void setStatus(PaperlessStatus status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }

    public Map<String, Object> getExtraJson() { return extraJson; }
    public void setExtraJson(Map<String, Object> extraJson) { this.extraJson = extraJson; }

    // 상태 Enum (DB CHECK 제약과 일치)
    public enum PaperlessStatus {
        PENDING, RECEIVED, IN_PROGRESS, APPROVED, REJECTED, CANCELED, COMPLETED
    }
}
