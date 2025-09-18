package kd.paperless.controller.post;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;

@Controller
@RequiredArgsConstructor
public class SignupPostController {

    private final UserRepository userRepository;

    @PostMapping("/signup")
    public String processSignup(
            @RequestParam String loginId,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            @RequestParam String userName,
            @RequestParam(required = false) String userBirth, // "YYYY-MM-DD"
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
            RedirectAttributes ra) {
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
        // 3) 아이디 중복 (실제 조회 기반)
        if (userRepository.findByLoginId(id).isPresent()) {
            ra.addFlashAttribute("error", "이미 사용 중인 아이디입니다.");
            return "redirect:/signup";
        }

        // 4) 엔티티 매핑
        User u = new User();
        u.setLoginId(id);
        u.setPasswordHash(pwd); // 개발용: 평문 저장
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

        // 5) 저장 (유니크 제약 충돌 대비)
        try {
            userRepository.save(u);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "이미 사용 중인 아이디(또는 이메일)입니다.");
            return "redirect:/signup";
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
