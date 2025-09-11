package kd.paperless.controller.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kd.paperless.controller.service.KakaoOAuthService;

@RestController
// @RequestMapping("/oauth/kakao")
public class KakaoOAuthController {

    private final KakaoOAuthService kakaoOAuthService;

    public KakaoOAuthController(KakaoOAuthService kakaoOAuthService) {
        this.kakaoOAuthService = kakaoOAuthService;
    }

    // Step 1: 로그인 리다이렉트
    @GetMapping("/oauth/kakao/login")
    public ResponseEntity<Void> redirectToKakaoLogin() {
        String kakaoLoginUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoOAuthService.getApiKey()
                + "&redirect_uri=" + kakaoOAuthService.getRedirectUri()
                + "&response_type=code";

        return ResponseEntity.status(302)
                .header("Location", kakaoLoginUrl)
                .build();
    }

    // Step 2: 콜백 처리
    @GetMapping("/oauth/kakao/callback")
    public ResponseEntity<?> kakaoCallback(@RequestParam String code) {
        String accessToken = kakaoOAuthService.getAccessToken(code);
        Map<String, Object> userInfo = kakaoOAuthService.getUserInfo(accessToken);

        // 사용자 정보 확인용 (실제론 여기서 회원가입 처리하거나 JWT 발급 등 진행)
        return ResponseEntity.ok(userInfo);
    }

}