package kd.paperless.dto.sinmungo;

import kd.paperless.entity.Sinmungo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SinmungoDetailDto {
  private Long smgId;
  private String title;
  private Long writerId;
  private LocalDateTime createdAt;
  private Long viewCount;
  private char noticeSms;
  private String noticeEmail;
  private Long adminId;
  private LocalDateTime answerDate;
  private String telNum;
  private String adminName;
  private String phoneNum;

  public static SinmungoDetailDto from(Sinmungo e) {
    return SinmungoDetailDto.builder()
        .smgId(e.getSmgId())
        .title(e.getTitle())
        .writerId(e.getWriterId())
        .createdAt(e.getCreatedAt())
        .viewCount(e.getViewCount() == null ? 0L : e.getViewCount())
        .noticeSms(e.getNoticeSms())
        .adminId(e.getAdminId())
        .answerDate(e.getAnswerDate())
        .telNum(e.getTelNum())
        .adminName(e.getAdmin() != null ? e.getAdmin().getAdminName() : null)
        .phoneNum(e.getAdmin() != null ? e.getAdmin().getPhoneNum() : null)
        .build();
  }
}
