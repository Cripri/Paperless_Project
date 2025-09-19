package kd.paperless.controller.post;

import static kd.paperless.account.PasswordResetSessionKeys.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

import kd.paperless.entity.User;
import kd.paperless.request.FindIdRequest;
import kd.paperless.request.FindPwRequest;
import kd.paperless.request.ResetPwRequest;
import kd.paperless.service.AccountFindService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountFindPostController {

    private final AccountFindService service;

    /** 아이디 찾기 */
    @PostMapping("/find-id")
    public String postFindId(FindIdRequest req, Model model) {
        model.addAttribute("activeTab", "id");
        return service.findLoginId(req)
                .map(loginId -> {
                    model.addAttribute("resultLoginId", loginId);
                    return "login/findAccount";
                })
                .orElseGet(() -> {
                    model.addAttribute("msg", "일치하는 회원이 없습니다.");
                    return "login/findAccount";
                });
    }

    /** 비밀번호 찾기 1단계: 아이디+이메일 검증 → 세션 저장 → /account/reset */
    @PostMapping("/find-pw")
    public String postFindPw(FindPwRequest req, Model model, HttpSession session) {
        model.addAttribute("activeTab", "pw");

        return service.verifyUser(req)
                .map(User::getId)
                .map(userId -> {
                    session.setAttribute(PW_RESET_UID, userId);
                    session.setAttribute(PW_RESET_EXPIRES, LocalDateTime.now().plusMinutes(10)); // 10분 유효
                    return "redirect:/account/reset";
                })
                .orElseGet(() -> {
                    model.addAttribute("msg", "일치하는 회원이 없습니다.");
                    return "login/findAccount";
                });
    }

    /** 비밀번호 재설정 저장 */
    @PostMapping("/reset")
    public String postReset(ResetPwRequest req, Model model, HttpSession session) {
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

        boolean ok = service.resetPasswordForUserId((Long) uid, req);

        // 사용 후 세션 정리
        session.removeAttribute(PW_RESET_UID);
        session.removeAttribute(PW_RESET_EXPIRES);

        model.addAttribute("activeTab", "id");
        model.addAttribute("msg", ok ? "비밀번호가 변경되었습니다. 로그인해 주세요." : "비밀번호 확인에 실패했습니다.");
        return "login/findAccount";
    }
}