package kd.paperless.controller.get;

import static kd.paperless.account.PasswordResetSessionKeys.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/account")
public class AccountFindGetController {

    // 찾기 페이지
    @GetMapping("/find")
    public String getFindPage(@RequestParam(value = "tab", defaultValue = "id") String tab, Model model) {
        model.addAttribute("activeTab", tab);
        return "login/findAccount";
    }

    // 비밀번호 재설정 페이지 (세션 검증)
    @GetMapping("/reset")
    public String getResetPage(Model model, HttpSession session) {
        Object uid = session.getAttribute(PW_RESET_UID);
        Object until = session.getAttribute(PW_RESET_EXPIRES);

        boolean invalid = (uid == null) ||
                          (until == null) ||
                          ((LocalDateTime) until).isBefore(LocalDateTime.now());

        if (invalid) {
            model.addAttribute("activeTab", "pw");
            model.addAttribute("msg", "비밀번호 재설정 시간이 만료되었거나 유효하지 않습니다. 다시 시도해 주세요.");
            session.removeAttribute(PW_RESET_UID);
            session.removeAttribute(PW_RESET_EXPIRES);
            return "login/findAccount";
        }

        return "login/resetPassword";
    }
}