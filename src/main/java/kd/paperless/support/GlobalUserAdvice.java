package kd.paperless.support;

import jakarta.servlet.http.HttpSession;
import kd.paperless.entity.User;
import kd.paperless.service.CustomUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "kd.paperless")
@Controller
public class GlobalUserAdvice {

  @ModelAttribute("loginName")
  public String loginName(Authentication authentication, HttpSession session) {
    // 1) Spring Security 인증 우선
    if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
      Object principal = authentication.getPrincipal();
      if (principal instanceof CustomUserDetails cud && cud.getUserName() != null) {
        return cud.getUserName(); // 실명
      }
    }
    // 2) 세션 병행(수동 로그인 유지 시)
    Object obj = session.getAttribute("loginUser");
    if (obj instanceof User u && u.getUserName() != null) {
      return u.getUserName();
    }
    return null;
  }
}
