package kd.paperless.controller.rest;

import jakarta.servlet.http.*;
import kd.paperless.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.util.UUID;

@Controller
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OauthRestController {

  private final SocialAuthService socialAuthService;
  private final SnsLinkService snsLinkService;
  private final UserDetailsByIdService userDetailsByIdService;
  private final SecurityContextRepository securityContextRepository;

  @GetMapping("/{provider}/login")
  public String start(@PathVariable String provider, HttpSession session) {
    String state = UUID.randomUUID().toString();
    session.setAttribute("OAUTH_STATE", state);
    String url = socialAuthService.buildAuthorizeUrl(provider.toUpperCase(), state);
    return "redirect:" + url;
  }

  @GetMapping("/{provider}/callback")
  public String callback(@PathVariable String provider,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(required = false, name = "error") String providerError,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         HttpSession session) {

    if (providerError != null || code == null) return "redirect:/login?error";

    String saved = (String) session.getAttribute("OAUTH_STATE");
    session.removeAttribute("OAUTH_STATE");
    if (saved == null || state == null || !saved.equals(state)) return "redirect:/login?error";

    String prov = provider.toUpperCase();

    String accessToken = socialAuthService.exchangeAccessToken(prov, code, state);
    String providerId  = socialAuthService.fetchProviderId(prov, accessToken);

    var linkedUserId = snsLinkService.findLinkedUserId(prov, providerId); // Optional<Long>
    if (linkedUserId.isPresent()) {
      forceLoginById(request, response, linkedUserId.get());   // ✅ PK로 바로 로그인
      return redirectToSavedOrDefault(request, response, "/portal"); // ✅ 변경 포인트
    }

    // 미연동 → 연동 대기
    snsLinkService.putPending(session, prov, providerId);
    return "redirect:/sns/connect";
  }

  /** PK로 강제 로그인: SecurityContext + 세션 저장 */
  private void forceLoginById(HttpServletRequest request, HttpServletResponse response, Long userId) {
    UserDetails user = userDetailsByIdService.loadUserById(userId);
    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);

    // ★ 세션에 SPRING_SECURITY_CONTEXT 저장 (이후 요청에서도 인증 유지)
    securityContextRepository.saveContext(context, request, response);
  }

  /** ✅ SavedRequest 있으면 그쪽으로, 없으면 기본값으로 */
  private String redirectToSavedOrDefault(HttpServletRequest request,
                                          HttpServletResponse response,
                                          String defaultUrl) {
    var cache = new HttpSessionRequestCache();
    SavedRequest saved = cache.getRequest(request, response);
    String target = (saved != null) ? saved.getRedirectUrl() : defaultUrl;
    return "redirect:" + target;
  }
}
