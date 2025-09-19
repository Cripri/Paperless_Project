package kd.paperless.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "SINMUNGO")
@SequenceGenerator(name = "sinmungo_seq", sequenceName = "SEQ_SINMUNGO", allocationSize = 1)
public class Sinmungo {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sinmungo_seq")
  @Column(name = "SMG_ID")
  private Long smgId;

  @Column(name = "TITLE", length = 200, nullable = false)
  private String title;

  @Column(name = "WRITER_ID", nullable = false)
  private Long writerId;

  @Lob
  @Column(name = "CONTENT", nullable = false)
  private String content;

  @Column(name = "CREATED_AT", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "VIEW_COUNT")
  private Long viewCount;

  @Column(name = "TEL_NUM", length = 20)
  private String telNum;

  @Column(name = "NOTICE_EMAIL", length = 200)
  private String noticeEmail;

  @Column(name = "NOTICE_SMS", length = 1)
  private char noticeSms;

  @Column(name = "POSTCODE", length = 10)
  private String postcode;

  @Column(name = "ADDR1", length = 200)
  private String addr1;

  @Column(name = "ADDR2", length = 200)
  private String addr2;

  @Column(name = "ADMIN_ID")
  private Long adminId;

  @Lob
  @Column(name = "ADMIN_ANSWER")
  private String adminAnswer;

  @Column(name = "ANSWER_DATE")
  private LocalDateTime answerDate;

  @Column(name = "STATUS", length = 20, nullable = false)
  private String status;

  @Column(name = "REJECT_REASON", length = 500)
  private String rejectReason;
}