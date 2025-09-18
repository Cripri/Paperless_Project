package kd.paperless.controller.post;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

import kd.paperless.repository.UserRepository;

@Controller
@RequiredArgsConstructor
public class LoginPostController {

    private final UserRepository userRepository;

    @PostMapping("/login")
    public String doLogin(String userId, String password, HttpSession session, Model model) {
        return userRepository.findByLoginIdAndPasswordHash(userId, password)
                .map(u -> {
                    session.setAttribute("loginUser", u);
                    return "redirect:/main/portal";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
                    return "login/login";
                });
    }
}
