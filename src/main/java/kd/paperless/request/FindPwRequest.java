package kd.paperless.request;

import lombok.Data;

@Data
public class FindPwRequest {
    private String loginId;
    private String email;
}