package kd.paperless.dto.sinmungo;

import kd.paperless.entity.Sinmungo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SinmungoListDto {
  private Long smgId;
  private String title;
  private Long writerId;
  private LocalDateTime createdAt;
  private String status;
  private Long viewCount;

  public static SinmungoListDto from(Sinmungo e) {
    return SinmungoListDto.builder()
        .smgId(e.getSmgId())
        .title(e.getTitle())
        .writerId(e.getWriterId())
        .createdAt(e.getCreatedAt())
        .status(e.getStatus())
        .viewCount(e.getViewCount() == null ? 0L : e.getViewCount())
        .build();
  }
}
