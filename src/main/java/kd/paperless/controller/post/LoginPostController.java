package kd.paperless.controller.post;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;

import kd.paperless.repository.UserRepository;
import kd.paperless.entity.User;

@Controller
@RequiredArgsConstructor
public class LoginPostController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public String doLogin(@RequestParam("userId") String userId,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        String id = userId == null ? "" : userId.trim();
        String pwd = password == null ? "" : password;

        var opt = userRepository.findByLoginId(id);
        if (opt.isEmpty()) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "login/login";
        }

        User u = opt.get();

        if (!passwordEncoder.matches(pwd, u.getPasswordHash())) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "login/login";
        }

        session.setAttribute("loginUser", u);

        return "redirect:/main/portal";
    }
}