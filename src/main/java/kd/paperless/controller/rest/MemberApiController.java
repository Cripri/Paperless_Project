package kd.paperless.controller.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import kd.paperless.account.PasswordPolicyUtil;
import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberApiController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/password/check")
    public Map<String, Object> checkPassword(@RequestParam("password") String rawPassword,
                                             @AuthenticationPrincipal(expression = "username") String loginId) {
        Map<String, Object> res = new HashMap<>();
        if (loginId == null) { res.put("success", false); res.put("message", "로그인이 필요합니다."); return res; }

        User user = userRepository.findByLoginId(loginId).orElse(null);
        if (user == null) { res.put("success", false); res.put("message", "사용자를 찾을 수 없습니다."); return res; }

        boolean ok = passwordEncoder.matches(rawPassword, user.getPasswordHash());
        res.put("success", ok);
        if (!ok) res.put("message", "비밀번호가 올바르지 않습니다.");
        return res;
    }
    
    @PostMapping("/password/change")
    public Map<String, Object> changePassword(@RequestParam String currentPassword,
                                              @RequestParam String newPassword,
                                              @RequestParam String confirmPassword,
                                              @AuthenticationPrincipal(expression = "username") String loginId) {
        Map<String, Object> res = new HashMap<>();
        if (loginId == null) { res.put("success", false); res.put("message", "로그인이 필요합니다."); return res; }

        User user = userRepository.findByLoginId(loginId).orElse(null);
        if (user == null) { res.put("success", false); res.put("message", "사용자를 찾을 수 없습니다."); return res; }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            res.put("success", false); res.put("message", "현재 비밀번호가 일치하지 않습니다."); return res;
        }
        if (!newPassword.equals(confirmPassword)) {
            res.put("success", false); res.put("message", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."); return res;
        }
        var vr = PasswordPolicyUtil.validate(newPassword);
        if (!vr.valid()) {
            res.put("success", false);
            res.put("message", "비밀번호 정책 위반: " + vr.firstMessageOrDefault("정책을 확인해 주세요."));
            return res;
        }

        if (PasswordPolicyUtil.isSameAsOld(newPassword, user.getPasswordHash(), passwordEncoder)) {
            res.put("success", false);
            res.put("message", "기존 비밀번호와 동일하게 변경할 수 없습니다.");
            return res;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        res.put("success", true);
        res.put("message", "비밀번호가 변경되었습니다.");
        return res;
    }
}