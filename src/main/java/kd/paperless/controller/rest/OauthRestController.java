package kd.paperless.controller.rest;

import jakarta.servlet.http.HttpSession;
import kd.paperless.service.SocialAuthService;
import kd.paperless.service.SnsLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OauthRestController {

  private final SocialAuthService socialAuthService; // 외부 OAuth
  private final SnsLinkService snsLinkService;       // 내부 연동

  /** 인가 시작: 네이버/카카오 인가 URL로 리다이렉트 */
  @GetMapping("/{provider}/login")
  public String start(@PathVariable String provider, HttpSession session) {
    String state = UUID.randomUUID().toString();
    session.setAttribute("OAUTH_STATE", state);
    String url = socialAuthService.buildAuthorizeUrl(provider.toUpperCase(), state);
    return "redirect:" + url;
  }

  /** 콜백: code/state → 토큰 교환 → 프로필 조회 → providerId 확보 */
  @GetMapping("/{provider}/callback")
  public String callback(@PathVariable String provider,
                         @RequestParam String code,
                         @RequestParam(required = false) String state,
                         HttpSession session) {

    // (선택) CSRF 방지: state 검증
    Object saved = session.getAttribute("OAUTH_STATE");
    if (saved instanceof String savedState) {
      if (state == null || !savedState.equals(state)) {
        throw new IllegalStateException("잘못된 state 값입니다.");
      }
    }

    String prov = provider.toUpperCase();

    // 토큰 교환 → 프로필 조회 → 소셜 고유 PK(providerId)
    String accessToken = socialAuthService.exchangeAccessToken(prov, code, state);
    String providerId  = socialAuthService.fetchProviderId(prov, accessToken);

    // 이미 연동 → 즉시 로그인
    var linkedUserId = snsLinkService.findLinkedUserId(prov, providerId);
    if (linkedUserId.isPresent()) {
      session.setAttribute("LOGIN_USER_ID", linkedUserId.get()); // 필요 시 SecurityContext로 교체
      return "redirect:/";
    }

    // 미연동 → 연동 대기 저장 후 연결 화면으로
    snsLinkService.putPending(session, prov, providerId);
    return "redirect:/sns/connect";
  }
}