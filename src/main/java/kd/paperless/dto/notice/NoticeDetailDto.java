package kd.paperless.dto.notice;

import kd.paperless.entity.Notice;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class NoticeDetailDto {
  private Long noticeId;
  private String title;
  private Long adminId;
  private LocalDateTime createdAt;
  private Long viewCount;
  private String content;

  public static NoticeDetailDto from(Notice e) {
    return NoticeDetailDto.builder()
        .noticeId(e.getNoticeId())
        .title(e.getTitle())
        .adminId(e.getAdminId())
        .createdAt(e.getCreatedAt())
        .viewCount(e.getViewCount() == null ? 0L : e.getViewCount())
        .content(e.getContent())
        .build();
  }
}
