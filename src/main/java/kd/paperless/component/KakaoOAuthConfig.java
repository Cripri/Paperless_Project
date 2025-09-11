package kd.paperless.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KakaoOAuthConfig {

    @Value("${kakao.api_key}")
    private String apiKey;

    @Value("${kakao.redirect_uri}")
    private String redirectUri;

    public String getApiKey() {
        return apiKey;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}