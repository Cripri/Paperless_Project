package kd.paperless.service;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import kd.paperless.component.KakaoOAuthConfig;

@Service
public class KakaoOAuthService {

    private final KakaoOAuthConfig config;
    private final RestTemplate restTemplate = new RestTemplate();

    public KakaoOAuthService(KakaoOAuthConfig config) {
        this.config = config;
    }

    public String getAccessToken(String code) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", config.getApiKey());
        params.add("redirect_uri", config.getRedirectUri());
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        Map<String, Object> body = response.getBody();
        if (body != null && body.containsKey("access_token")) {
            return (String) body.get("access_token");
        }

        throw new RuntimeException("Access token 요청 실패");
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            userInfoUrl,
            HttpMethod.GET,
            request,
            Map.class
        );

        return response.getBody();
    }

    public String getApiKey() {
        return config.getApiKey();
    }

    public String getRedirectUri() {
        return config.getRedirectUri();
    }
}