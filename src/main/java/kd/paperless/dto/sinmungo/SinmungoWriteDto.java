package kd.paperless.dto.sinmungo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import kd.paperless.entity.Sinmungo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@Builder
public class SinmungoWriteDto {

  @NotBlank(message = "제목은 필수입니다.")
  private String title;

  @NotBlank(message = "휴대폰 번호는 필수입니다.")
  @Pattern(regexp = "^[0-9]{9,12}$", message = "휴대폰 번호는 숫자만 입력하세요.")
  private String telNum;

  @NotBlank(message = "우편번호는 필수입니다.")
  private String postcode;

  @NotBlank(message = "주소는 필수입니다.")
  private String addr1;

  @NotBlank(message = "상세 주소는 필수입니다.")
  private String addr2;

  @NotBlank(message = "내용은 필수입니다.")
  private String content;

  private String noticeSms;

  @Email(message = "이메일 형식이 올바르지 않습니다.")
  private String noticeEmail;

  @Pattern(regexp = "^$|^[0-9]{9,12}$", message = "전화번호는 숫자만 입력하세요.")
  private String telNum2;

  private List<MultipartFile> files;

  public void normalize() {
    if (title != null)
      title = title.trim();
    if (noticeEmail != null && noticeEmail.isBlank())
      noticeEmail = null;
    if (noticeSms == null || noticeSms.isBlank())
      noticeSms = "N";
  }

  public static SinmungoWriteDto fromEntity(Sinmungo e) {
    return SinmungoWriteDto.builder()
        .title(e.getTitle())
        .telNum(e.getTelNum())
        .postcode(e.getPostcode())
        .addr1(e.getAddr1())
        .addr2(e.getAddr2())
        .content(e.getContent())
        .noticeEmail(e.getNoticeEmail())
        .noticeSms(String.valueOf(e.getNoticeSms()))
        .build();
  }
}
