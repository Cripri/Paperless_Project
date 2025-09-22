package kd.paperless.request;

import lombok.Data;

@Data
public class ResetPwRequest {
    private String newPassword;
    private String confirmPassword;
}