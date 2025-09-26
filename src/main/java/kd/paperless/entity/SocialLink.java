package kd.paperless.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
  name = "SOCIAL_LINK",
  uniqueConstraints = @UniqueConstraint(
      name = "UK_SOCIAL_LINK_PROVIDER_ID",
      columnNames = {"PROVIDER","PROVIDER_ID"}
  )
)
@SequenceGenerator(
  name = "social_link_seq",
  sequenceName = "SEQ_SOCIAL_LINK",
  allocationSize = 1
)
public class SocialLink {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "social_link_seq")
  @Column(name = "LINK_ID")
  private Long linkId;

  @Column(name = "USER_ID", nullable = false)
  private Long userId;

  @Column(name = "PROVIDER", length = 20, nullable = false)
  private String provider; // NAVER | KAKAO

  @Column(name = "PROVIDER_ID", length = 200, nullable = false)
  private String providerId; // 소셜 고유 ID

  @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}