package kd.paperless.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthLoginHelper {

    private final AuthenticationManager authenticationManager;
    // 세션 기반이라면 HttpSessionSecurityContextRepository 사용
    private final SecurityContextRepository contextRepo = new HttpSessionSecurityContextRepository();

    /** 가입 직후 아이디/비번으로 즉시 로그인 */
    public void login(HttpServletRequest req, HttpServletResponse res,
                      String loginId, String rawPassword) {
        UsernamePasswordAuthenticationToken input =
                new UsernamePasswordAuthenticationToken(loginId, rawPassword);
        Authentication authenticated = authenticationManager.authenticate(input);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        contextRepo.saveContext(context, req, res);
    }
}
