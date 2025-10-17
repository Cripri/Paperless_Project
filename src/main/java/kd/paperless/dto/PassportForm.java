package kd.paperless.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PassportForm {

    // ① 기본 선택
    @NotBlank(message = "여권 종류를 선택하세요.")
    private String passportType;   // NORMAL, OFFICIAL, DIPLOMAT, EMERGENCY, TRAVEL_CERT

    private String travelMode;     // ROUND, ONEWAY (TRAVEL_CERT일 때만)

    @NotNull(message = "면수를 선택하세요.")
    private Integer pageCount;     // 26, 58

    @NotBlank(message = "기간을 선택하세요.")
    private String validity;       // 10Y, 1Y_SINGLE, REMAINING

    // ② 인적 사항
    @NotBlank(message = "한글 성명을 입력하세요.")
    private String koreanName;

    @Pattern(regexp = "^\\d{6}$", message = "주민등록번호 앞 6자리를 입력하세요.")
    private String rrnFront;

    @Pattern(regexp = "^\\d{7}$", message = "주민등록번호 뒤 7자리를 입력하세요.")
    private String rrnBack;

    @NotBlank(message = "연락처를 입력하세요.")
    private String phone;

    private String emergencyName;
    private String emergencyRelation;
    private String emergencyPhone;

    @NotBlank(message = "영문 성(Last Name)을 입력하세요.")
    private String engLastName;

    @NotBlank(message = "영문 이름(First Name)을 입력하세요.")
    private String engFirstName;

    private String spouseEngLastName;

    @NotBlank(message = "점자여권 여부를 선택하세요.")
    private String braillePassport; // Y / N

    // ③ 우편 배송
    @NotBlank(message = "우편배송 희망 여부를 선택하세요.")
    private String deliveryWanted;  // Y / N

    private String deliveryPostcode;
    private String deliveryAddress1;
    private String deliveryAddress2;

    // ④ 사진(Thymeleaf 필드 에러 바인딩을 위해 DTO에 포함)
    private MultipartFile photoFile;
}
