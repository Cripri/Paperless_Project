package kd.paperless.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PassportForm {
  private String passportType;      // NORMAL, OFFICIAL, DIPLOMAT, EMERGENCY, TRAVEL_CERT
  private String travelMode;        // ROUND, ONEWAY
  private Integer pageCount;        // 26 or 58
  private String validity;          // 10Y, 1Y_SINGLE, REMAINING

  private MultipartFile photoFile;  // 여권사진

  private String koreanName;
  private String rrnFront;
  private String rrnBack;
  private String phone;

  private String emergencyName;
  private String emergencyRelation;
  private String emergencyPhone;

  private String engLastName;
  private String engFirstName;
  private String spouseEngLastName;

  private String braillePassport;   // Y / N
  private String deliveryWanted;    // Y / N
  private String deliveryPostcode;
  private String deliveryAddress1;
  private String deliveryAddress2;
}
