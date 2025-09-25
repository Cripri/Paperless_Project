package kd.paperless.controller.rest;

import jakarta.servlet.http.HttpSession;
import kd.paperless.service.SocialAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/oauth")
public class OAuthController {

  private final SocialAuthService socialAuthService;

  /** 1) 인가 시작 (네이버/카카오 공통) */
  @GetMapping("/{provider}/login")
  public String start(@PathVariable String provider, HttpSession session) {
    String upper = provider.toUpperCase();           // "NAVER" | "KAKAO"
    String state = UUID.randomUUID().toString();     // CSRF 방지
    session.setAttribute("OAUTH_STATE_" + upper, state);
    String url = socialAuthService.buildAuthorizeUrl(upper, state);
    return "redirect:" + url;
  }

  /** 2) 네이버 콜백 */
  @GetMapping("/naver/callback")
  public String naverCallback(@RequestParam String code, @RequestParam String state, HttpSession session) {
    return handleCallback("NAVER", code, state, session);
  }

  /** 3) 카카오 콜백 */
  @GetMapping("/kakao/callback")
  public String kakaoCallback(@RequestParam String code,
                              @RequestParam(required = false) String state,
                              HttpSession session) {
    return handleCallback("KAKAO", code, state, session);
  }

  /** 공통 콜백 처리 */
  private String handleCallback(String providerUpper, String code, String state, HttpSession session) {
    // (a) state 검증 (네이버 필수, 카카오는 보통 포함)
    String saved = (String) session.getAttribute("OAUTH_STATE_" + providerUpper);
    if (saved == null || ("NAVER".equals(providerUpper) && !saved.equals(state))) {
      throw new IllegalStateException("Invalid OAuth state");
    }

    // (b) 토큰 교환 → (c) 프로필 조회 → providerId
    String accessToken = socialAuthService.exchangeAccessToken(providerUpper, code, state);
    String providerId  = socialAuthService.fetchProviderId(providerUpper, accessToken);

    // (d) 미연동 처리: 연결 페이지로 전달할 정보 세션에 저장
    session.setAttribute("PENDING_PROVIDER", providerUpper);
    session.setAttribute("PENDING_PROVIDER_ID", providerId);

    // (e) 이미 연동된 계정이면 여기서 바로 로그인 처리도 가능(다음 단계에서 붙일 예정)
    return "redirect:/sns/connect";
  }
}
