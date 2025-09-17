package kd.paperless.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import kd.paperless.converter.JsonMapConverter;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="paperless_doc")
@Getter @Setter
public class PaperlessDoc {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_paperless_pl_id")
  @SequenceGenerator(name="seq_paperless_pl_id", sequenceName="SEQ_PAPERLESS_PL_ID", allocationSize=100)
  private Long plId;

  private Long userId;

  @Column(length=1, nullable=false)
  private String consentYn = "N";

  @Column(length=20, nullable=false)
  private String status = "PENDING";

  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable=false)
  private Date submittedAt = new Date();

  @Temporal(TemporalType.TIMESTAMP)
  private Date processedAt;

  private Long adminId;

  @Column(length=30, nullable=false)
  private String docType;

  @Convert(converter = JsonMapConverter.class)
  @Column(columnDefinition = "CLOB CHECK (extra_json IS JSON)", nullable=false)
  private Map<String,Object> extraJson = new HashMap<>();
}
