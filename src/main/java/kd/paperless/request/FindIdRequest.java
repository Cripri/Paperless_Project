package kd.paperless.request;

import lombok.Data;

@Data
public class FindIdRequest {
    private String name;
    private String email;
}