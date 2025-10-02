package kd.paperless.controller.get;

import java.util.Map;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import kd.paperless.repository.UserRepository;
import kd.paperless.service.SnsLinkService;

@Controller
@RequiredArgsConstructor
public class SignupGetController {

    private final UserRepository userRepository;
    private final SnsLinkService snsLinkService;

    // 회원가입 화면
    @GetMapping("/signup")
    public String showSignupForm(@RequestParam(value = "sns", required = false) String sns,
            HttpSession session,
            Model model) {

        // 세션에 SNS 연동 대기가 있거나, ?sns=1 로 접근한 경우만 snsFlow = true
        boolean snsFlow = "1".equals(sns) || snsLinkService.getPending(session).isPresent();
        model.addAttribute("snsFlow", snsFlow);

        return "login/signup";
    }

    // 아이디 중복확인
    @GetMapping("/api/users/check-id")
    @ResponseBody
    public Map<String, Object> checkId(@RequestParam String loginId) {
        String id = loginId == null ? "" : loginId.trim();
        boolean exists = !id.isEmpty() && userRepository.existsByLoginId(id);
        return Map.of("available", !exists);
    }
}