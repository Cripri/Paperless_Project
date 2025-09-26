package kd.paperless.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class SocialAuthService {

  private final RestClient rest = RestClient.create();

  // ====== application(-private).properties 키와 매핑 ======
  @Value("${naver.client.id:}")     private String naverId;
  @Value("${naver.client.secret:}") private String naverSecret;
  @Value("${naver.redirect.uri:}")  private String naverRedirectUri;

  @Value("${kakao.api_key:}")       private String kakaoApiKey;        // REST API Key
  @Value("${kakao.client.secret:}") private String kakaoClientSecret; // 선택값 없으면 비워둠
  @Value("${kakao.redirect_uri:}")  private String kakaoRedirectUri;

  // ====== 고정 엔드포인트: 코드에 상수로 둡니다 ======
  private static final String NAVER_AUTH_URL   = "https://nid.naver.com/oauth2.0/authorize";
  private static final String NAVER_TOKEN_URL  = "https://nid.naver.com/oauth2.0/token";
  private static final String NAVER_PROFILE_URL= "https://openapi.naver.com/v1/nid/me";

  private static final String KAKAO_AUTH_URL   = "https://kauth.kakao.com/oauth/authorize";
  private static final String KAKAO_TOKEN_URL  = "https://kauth.kakao.com/oauth/token";
  private static final String KAKAO_PROFILE_URL= "https://kapi.kakao.com/v2/user/me";

  /** 인가 URL 생성 */
  public String buildAuthorizeUrl(String providerUpper, String state) {
    switch (providerUpper) {
      case "NAVER":
        return NAVER_AUTH_URL
            + "?response_type=code"
            + "&client_id=" + enc(naverId)
            + "&redirect_uri=" + enc(naverRedirectUri)
            + "&state=" + enc(state);
      case "KAKAO":
        return KAKAO_AUTH_URL
            + "?response_type=code"
            + "&client_id=" + enc(kakaoApiKey)
            + "&redirect_uri=" + enc(kakaoRedirectUri)
            + "&state=" + enc(state); // 카카오도 state 사용 가능
      default:
        throw new IllegalArgumentException("Unsupported provider: " + providerUpper);
    }
  }

  /** 액세스 토큰 교환 */
  public String exchangeAccessToken(String providerUpper, String code, String stateOrNull) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    String tokenUrl;

    if ("NAVER".equals(providerUpper)) {
      tokenUrl = NAVER_TOKEN_URL;
      form.add("grant_type", "authorization_code");
      form.add("client_id", naverId);
      form.add("client_secret", naverSecret);
      form.add("code", code);
      form.add("state", stateOrNull == null ? "" : stateOrNull);
    } else if ("KAKAO".equals(providerUpper)) {
      tokenUrl = KAKAO_TOKEN_URL;
      form.add("grant_type", "authorization_code");
      form.add("client_id", kakaoApiKey);
      if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
        form.add("client_secret", kakaoClientSecret);
      }
      form.add("code", code);
      form.add("redirect_uri", kakaoRedirectUri);
    } else {
      throw new IllegalArgumentException("Unsupported provider: " + providerUpper);
    }

    Map<?,?> res = rest.post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(Map.class);

    if (res == null || res.get("access_token") == null) {
      throw new IllegalStateException("Access token not found");
    }
    return String.valueOf(res.get("access_token"));
  }

  /** 프로필 조회 → 고유 ID 추출 */
  public String fetchProviderId(String providerUpper, String accessToken) {
    if ("NAVER".equals(providerUpper)) {
      NaverProfile p = rest.get()
          .uri(NAVER_PROFILE_URL)
          .header("Authorization", "Bearer " + accessToken)
          .retrieve()
          .body(NaverProfile.class);
      if (p == null || !"00".equals(p.resultcode) || p.response == null || p.response.id == null) {
        throw new IllegalStateException("NAVER profile error");
      }
      return p.response.id;
    } else if ("KAKAO".equals(providerUpper)) {
      KakaoProfile p = rest.get()
          .uri(KAKAO_PROFILE_URL)
          .header("Authorization", "Bearer " + accessToken)
          .retrieve()
          .body(KakaoProfile.class);
      if (p == null || p.id == null) {
        throw new IllegalStateException("KAKAO profile error");
      }
      return String.valueOf(p.id);
    }
    throw new IllegalArgumentException("Unsupported provider: " + providerUpper);
  }

  private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

  // ===== 응답 모델 =====
  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class NaverProfile {
    public String resultcode;
    public String message;
    public NaverUser response;
  }
  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class NaverUser { public String id; }

  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class KakaoProfile { public Long id; }
}