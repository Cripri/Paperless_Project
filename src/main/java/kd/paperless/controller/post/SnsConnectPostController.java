package kd.paperless.controller.post;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kd.paperless.service.SnsLinkService;
import kd.paperless.service.UserDetailsByIdService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sns")
@RequiredArgsConstructor
public class SnsConnectPostController {

  private final SnsLinkService snsLinkService;

  // ✅ 추가 주입
  private final UserDetailsByIdService userDetailsByIdService;
  private final SecurityContextRepository securityContextRepository;

  // 기존 계정과 연결 (아이디/비밀번호 확인 → social_link 저장 → 로그인)
  @PostMapping("/connect/existing")
  public String connectExisting(@RequestParam String loginId,
                                @RequestParam String password,
                                HttpSession session,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                RedirectAttributes ra) {
    var pending = snsLinkService.getPending(session)
        .orElseThrow(() -> new IllegalStateException("연동 대기 중인 소셜 정보가 없습니다."));

    try {
      Long userId = snsLinkService.connectExisting(
          loginId, password, pending.provider(), pending.providerId());

      // ❌ session.setAttribute("LOGIN_USER_ID", userId);
      // ✅ 연동 직후 즉시 강제 로그인
      forceLoginById(request, response, userId);

      snsLinkService.clearPending(session);
      return "redirect:/portal"; // 필요시 SavedRequest로 변경 가능
    } catch (IllegalArgumentException | IllegalStateException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/sns/connect";
    }
  }

  // 회원가입 완료 후 연동 (신규 PK 전달)
  @PostMapping("/connect/signup-complete")
  public String connectAfterSignup(@RequestParam("newUserId") Long newUserId,
                                   HttpSession session,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
    var pending = snsLinkService.getPending(session)
        .orElseThrow(() -> new IllegalStateException("연동 대기 중인 소셜 정보가 없습니다. 다시 시도해 주세요."));

    snsLinkService.connectAfterSignup(newUserId, pending.provider(), pending.providerId());

    // ❌ session.setAttribute("LOGIN_USER_ID", newUserId);
    // ✅ 연동 직후 즉시 강제 로그인
    forceLoginById(request, response, newUserId);

    snsLinkService.clearPending(session);
    return "redirect:/portal";
  }

  /** ✅ PK로 즉시 로그인 + 세션 저장 */
  private void forceLoginById(HttpServletRequest request, HttpServletResponse response, Long userId) {
    UserDetails user = userDetailsByIdService.loadUserById(userId);
    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    // ★ 세션에 SPRING_SECURITY_CONTEXT 저장 (이후 요청에서도 인증 유지)
    securityContextRepository.saveContext(context, request, response);
  }
}