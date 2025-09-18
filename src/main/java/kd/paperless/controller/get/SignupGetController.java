// 회원가입 화면 + 아이디 중복확인 API
package kd.paperless.controller.get;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import kd.paperless.repository.UserRepository;

@Controller
public class SignupGetController {

    private final UserRepository userRepository;
    public SignupGetController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/signup")
    public String showSignupForm() {
        return "login/signup";          // templates/login/signup.html
    }

    // /api/users/check-id?loginId=...
    @GetMapping("/api/users/check-id")
    @ResponseBody
    public Map<String, Object> checkId(@RequestParam String loginId) {
        String id = loginId == null ? "" : loginId.trim();
        boolean exists = !id.isEmpty() && userRepository.existsByLoginId(id);
        return Map.of("available", !exists);
    }
}