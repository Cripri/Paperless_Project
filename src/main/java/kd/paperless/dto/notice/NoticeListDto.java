package kd.paperless.dto.notice;

import kd.paperless.entity.Notice;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class NoticeListDto {
  private Long noticeId;
  private String title;
  private LocalDateTime createdAt;
  private Long viewCount;
  private char isPinned;

  public static NoticeListDto from(Notice e) {
    return NoticeListDto.builder()
        .noticeId(e.getNoticeId())
        .title(e.getTitle())
        .createdAt(e.getCreatedAt())
        .viewCount(e.getViewCount() == null ? 0L : e.getViewCount())
        .isPinned(e.getIsPinned())
        .build();
  }
}
