package kd.paperless.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "fileId")
@Entity
@Table(name = "ATTACHMENTS", indexes = {
    @Index(name = "IDX_ATTACH_TARGET", columnList = "TARGET_TYPE,TARGET_ID")
})
public class Attachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "FILE_ID")
  private Long fileId;

  @Column(name = "TARGET_TYPE", length = 30, nullable = false)
  private String targetType;

  @Column(name = "TARGET_ID", nullable = false)
  private Long targetId;

  @Column(name = "FILE_URI", length = 500, nullable = false)
  private String fileUri;

  @Column(name = "FILE_NAME", length = 255, nullable = false)
  private String fileName;

  @Column(name = "MIME_TYPE", length = 100)
  private String mimeType;

  @Column(name = "FILE_SIZE", nullable = false)
  private Long fileSize;

  @Column(name = "CREATED_AT", insertable = false, updatable = false)
  private LocalDateTime createdAt;
}