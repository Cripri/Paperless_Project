package kd.paperless.controller.post;

import jakarta.servlet.http.HttpSession;
import kd.paperless.service.SnsLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/sns")
@RequiredArgsConstructor
public class SnsConnectPostController {

  private final SnsLinkService snsLinkService;

  // 기존 계정과 연결 (아이디/비밀번호 확인 → social_link 저장 → 로그인)
  @PostMapping("/connect/existing")
  public String connectExisting(@RequestParam String loginId,
      @RequestParam String password,
      HttpSession session,
      RedirectAttributes ra) {
    var pending = snsLinkService.getPending(session)
        .orElseThrow(() -> new IllegalStateException("연동 대기 중인 소셜 정보가 없습니다."));

    try {
      Long userId = snsLinkService.connectExisting(
          loginId, password, pending.provider(), pending.providerId());
      session.setAttribute("LOGIN_USER_ID", userId);
      snsLinkService.clearPending(session);
      return "redirect:/";
    } catch (IllegalArgumentException | IllegalStateException ex) {
      // 🔥 여기서 사용자 친화적 메시지를 플래시로 전달
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/sns/connect";
    }
  }

  // 회원가입 완료 후 연동 (신규 PK 전달)
  @PostMapping("/connect/signup-complete")
  public String connectAfterSignup(@RequestParam("newUserId") Long newUserId,
      HttpSession session) {
    var pending = snsLinkService.getPending(session)
        .orElseThrow(() -> new IllegalStateException("연동 대기 중인 소셜 정보가 없습니다. 다시 시도해 주세요."));

    snsLinkService.connectAfterSignup(newUserId, pending.provider(), pending.providerId());

    session.setAttribute("LOGIN_USER_ID", newUserId); // 필요 시 SecurityContext로 교체
    snsLinkService.clearPending(session);
    return "redirect:/";
  }
}
