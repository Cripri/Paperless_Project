package kd.paperless.controller.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/portal/member")
@RequiredArgsConstructor
public class MemberController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 비밀번호 확인 API */
    @PostMapping("/checkPw.do")
    public Map<String, Object> checkPassword(
            @RequestParam("password") String rawPassword,
            @AuthenticationPrincipal(expression = "username") String loginId // 또는 principal 객체 커스텀 필드
    ) {
        Map<String, Object> res = new HashMap<>();

        if (loginId == null) {
            res.put("success", false);
            res.put("message", "로그인이 필요합니다.");
            return res;
        }

        // 로그인 사용자를 DB에서 조회 (loginId 기준)
        User user = userRepository.findByLoginId(loginId)
                .orElse(null);

        if (user == null) {
            res.put("success", false);
            res.put("message", "사용자를 찾을 수 없습니다.");
            return res;
        }

        // BCrypt 검증
        boolean ok = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        res.put("success", ok);
        if (!ok) res.put("message", "비밀번호가 올바르지 않습니다.");

        return res;
    }
}
