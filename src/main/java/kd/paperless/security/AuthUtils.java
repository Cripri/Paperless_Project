package kd.paperless.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils {

    public String redirectToSavedOrDefault(HttpServletRequest request,
                                           HttpServletResponse response,
                                           String defaultUrl) {
        var cache = new HttpSessionRequestCache();
        SavedRequest saved = cache.getRequest(request, response);
        String target = (saved != null ? saved.getRedirectUrl() : defaultUrl);
        return "redirect:" + target;
    }
}
