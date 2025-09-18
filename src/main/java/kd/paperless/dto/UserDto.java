package kd.paperless.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

import kd.paperless.entity.User;

@Data
@Builder
public class UserDto {
    private Long id;
    private String loginId;
    private String userName;
    private LocalDate userBirth;
    private String phoneNum;
    private String telNum;
    private String email;
    private String postcode;
    private String addr1;
    private String addr2;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;

    // 엔티티 -> DTO 변환
    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .userName(user.getUserName())
                .userBirth(user.getUserBirth())
                .phoneNum(user.getPhoneNum())
                .telNum(user.getTelNum())
                .email(user.getEmail())
                .postcode(user.getPostcode())
                .addr1(user.getAddr1())
                .addr2(user.getAddr2())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .status(user.getStatus())
                .build();
    }
}
