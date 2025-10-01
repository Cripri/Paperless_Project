package kd.paperless.controller.post;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kd.paperless.account.PasswordPolicyUtil;
import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;
import kd.paperless.service.SnsLinkService;

@Controller
@RequiredArgsConstructor
public class SignupPostController {

    @Autowired PasswordEncoder encoder;

    private final UserRepository userRepository;
    private final SnsLinkService snsLinkService;

    @PostMapping("/signup")
    public String processSignup(
            @RequestParam String loginId,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            @RequestParam String userName,
            @RequestParam(required = false) String userBirth,
            @RequestParam(required = false) String phoneMid,
            @RequestParam(required = false) String phoneLast,
            @RequestParam(required = false) String telArea,
            @RequestParam(required = false) String telMid,
            @RequestParam(required = false) String telLast,
            @RequestParam(required = false) String emailId,
            @RequestParam(required = false) String emailDomain,
            @RequestParam(required = false) String postcode,
            @RequestParam(required = false) String addr1,
            @RequestParam(required = false) String addr2,
            RedirectAttributes ra,
            HttpSession session) {

        // 0) 입력 정규화
        String id = nz(loginId).trim();
        String pwd = nz(password);
        String pwd2 = nz(passwordConfirm);
        String name = nz(userName).trim();

        // 1) 필수값
        if (id.isEmpty() || pwd.isEmpty() || name.isEmpty()) {
            ra.addFlashAttribute("error", "아이디/비밀번호/이름은 필수입니다.");
            return "redirect:/signup";
        }
        // 2) 비밀번호 확인
        if (!pwd.equals(pwd2)) {
            ra.addFlashAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "redirect:/signup";
        }
        
        var vr = PasswordPolicyUtil.validate(pwd);
        if (!vr.valid()) {
            ra.addFlashAttribute("error", "비밀번호 정책 위반: " + vr.firstMessageOrDefault("정책을 확인해 주세요."));
            return "redirect:/signup";
        }

        // 3) 아이디 중복
        if (userRepository.findByLoginId(id).isPresent()) {
            ra.addFlashAttribute("error", "이미 사용 중인 아이디입니다.");
            return "redirect:/signup";
        }

        // 4) 엔티티 매핑
        User u = new User();
        u.setLoginId(id);
        u.setPasswordHash(encoder.encode(pwd)); // 운영: 반드시 해시 저장
        u.setUserName(name);

        if (!isBlank(userBirth)) {
            try {
                LocalDate birth = LocalDate.parse(userBirth.trim()); // "1990-05-10"
                u.setUserBirth(birth);
            } catch (DateTimeParseException e) {
                ra.addFlashAttribute("error", "올바른 생년월일을 입력해주세요.");
                return "redirect:/signup";
            }
        }
        if (!isBlank(phoneMid) && !isBlank(phoneLast)) {
            u.setPhoneNum("010-" + phoneMid.trim() + "-" + phoneLast.trim());
        }
        if (!isBlank(telArea) && !"선택".equals(telArea)) {
            u.setTelNum(telArea + "-" + nz(telMid).trim() + "-" + nz(telLast).trim());
        }
        if (!isBlank(emailId) && !isBlank(emailDomain)) {
            u.setEmail(emailId.trim() + "@" + emailDomain.trim());
        }
        u.setPostcode(nz(postcode).trim());
        u.setAddr1(nz(addr1).trim());
        u.setAddr2(nz(addr2).trim());

        u.setStatus("ACTIVE");
        u.setCreatedAt(LocalDateTime.now());

        // 5) 저장
        try {
            userRepository.save(u);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "이미 사용 중인 아이디(또는 이메일)입니다.");
            return "redirect:/signup";
        }

        // 6) SNS 연동 대기(PENDING)가 있으면 즉시 연동 저장
        var pendingOpt = snsLinkService.getPending(session);
        if (pendingOpt.isPresent()) {
            var pending = pendingOpt.get();

            Long newUserId = u.getId(); // PK 접근

            snsLinkService.connectAfterSignup(newUserId, pending.provider(), pending.providerId());
            snsLinkService.clearPending(session);

            return "redirect:/login?joined=1&linkedSns=1";
        }

        return "redirect:/login?joined=1";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
