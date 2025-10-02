package kd.paperless.controller.rest;

import static kd.paperless.account.PasswordResetSessionKeys.*;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/account/api")
public class AccountResetApiController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 재설정 세션의 사용자와 새 비밀번호가 동일한지 검사 */
    @PostMapping("/check-same")
    public Map<String, Object> checkSame(@RequestParam("newPassword") String newPassword,
                                         HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        Object uid = session.getAttribute(PW_RESET_UID);
        if (uid == null) {
            res.put("ok", false);
            res.put("same", false);
            res.put("message", "재설정 세션이 유효하지 않습니다.");
            return res;
        }

        User user = userRepository.findById((Long) uid).orElse(null);
        if (user == null) {
            res.put("ok", false);
            res.put("same", false);
            res.put("message", "사용자를 찾을 수 없습니다.");
            return res;
        }

        boolean same = passwordEncoder.matches(newPassword, user.getPasswordHash());
        res.put("ok", true);
        res.put("same", same);
        return res;
    }
}