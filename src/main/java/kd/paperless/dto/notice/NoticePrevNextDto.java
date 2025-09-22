package kd.paperless.dto.notice;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NoticePrevNextDto {
    private Long noticeId;
    private String title;
}