package kd.paperless.entity;

import jakarta.persistence.*;
import kd.paperless.config.converter.JsonMapConverter;
import lombok.Getter;
import lombok.Setter;

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
@Getter @Setter
public class PaperlessDoc {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paperless_pl_id_seq")
    @Column(name = "pl_id", nullable = false)
    private Long plId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "consent_yn", nullable = false, length = 1)
    private char consentYn = 'N';

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaperlessStatus status = PaperlessStatus.PENDING;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType;

    @Convert(converter = JsonMapConverter.class)
    @Lob
    @JdbcTypeCode(SqlTypes.CLOB) // Hibernate 6.x에서 CLOB 타입 명시
    @Column(name = "extra_json", nullable = false, columnDefinition = "CLOB")
    private Map<String, Object> extraJson = new HashMap<>();

    public PaperlessDoc() {}

    public PaperlessDoc(Long userId, String docType) {
        this.userId = userId;
        this.docType = docType;
    }

    // 상태 Enum (DB CHECK 제약과 일치)
    public enum PaperlessStatus {
        PENDING, RECEIVED, IN_PROGRESS, APPROVED, REJECTED, CANCELED, COMPLETED
    }
}
