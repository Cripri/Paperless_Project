package kd.paperless.controller.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/oauth/naver")
public class NaverLoginController {

    @Value("${naver.client.id}")
    private String clientId;

    @Value("${naver.client.secret}")
    private String clientSecret;

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam String code, @RequestParam String state) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 1. Access Token 요청
            String tokenUrl = "https://nid.naver.com/oauth2.0/token"
                    + "?grant_type=authorization_code"
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + "&code=" + code
                    + "&state=" + state;

            ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> tokenBody = tokenResponse.getBody();
            if (tokenBody == null || !tokenBody.containsKey("access_token")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("토큰 응답이 유효하지 않습니다.");
            }

            String accessToken = (String) tokenBody.get("access_token");

            // 2. 사용자 정보 요청
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> userInfoResponse = restTemplate.exchange(
                    "https://openapi.naver.com/v1/nid/me",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = userInfoResponse.getBody();
            if (responseBody == null || !responseBody.containsKey("response")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("사용자 정보가 없습니다.");
            }

            Map<String, Object> userInfo = (Map<String, Object>) responseBody.get("response");

            // 여기에 회원가입/로그인 처리 로직 추가 가능
            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("네이버 로그인 실패: " + e.getMessage());
        }
    }
}
