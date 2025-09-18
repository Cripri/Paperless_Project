package kd.paperless.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SignupRequest {
    private String loginId;
    private String password;
    private String userName;
    private LocalDate userBirth;
    private String phoneNum;
    private String telNum;
    private String email;
    private String postcode;
    private String addr1;
    private String addr2;
}