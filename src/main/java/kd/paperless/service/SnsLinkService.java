package kd.paperless.service;

import jakarta.servlet.http.HttpSession;
import kd.paperless.entity.SocialLink;
import kd.paperless.repository.SocialLinkRepository;
import kd.paperless.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class SnsLinkService {

    private final SocialLinkRepository socialLinkRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SnsLinkService(SocialLinkRepository socialLinkRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.socialLinkRepository = socialLinkRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final String PENDING_PROVIDER = "PENDING_PROVIDER";
    private static final String PENDING_PROVIDER_ID = "PENDING_PROVIDER_ID";

    public Optional<Long> findLinkedUserId(String provider, String providerId) {
        return socialLinkRepository.findByProviderAndProviderId(provider, providerId)
                .map(SocialLink::getUserId);
    }

    public void putPending(HttpSession session, String provider, String providerId) {
        session.setAttribute(PENDING_PROVIDER, provider);
        session.setAttribute(PENDING_PROVIDER_ID, providerId);
    }

    public Optional<Pending> getPending(HttpSession session) {
        Object p = session.getAttribute(PENDING_PROVIDER);
        Object u = session.getAttribute(PENDING_PROVIDER_ID);
        if (p instanceof String provider && u instanceof String providerId) {
            return Optional.of(new Pending(provider, providerId));
        }
        return Optional.empty();
    }

    public void clearPending(HttpSession session) {
        session.removeAttribute(PENDING_PROVIDER);
        session.removeAttribute(PENDING_PROVIDER_ID);
    }

    public void link(Long userId, String provider, String providerId) {
        socialLinkRepository.findByProviderAndProviderId(provider, providerId)
                .ifPresent(ex -> {
                    throw new IllegalStateException("이 소셜 계정은 이미 다른 사용자에 연동되어 있습니다.");
                });
        // (정책) 한 유저는 한 소셜만
        if (socialLinkRepository.existsByUserId(userId)) {
            throw new IllegalStateException("이미 다른 소셜 계정과 연동되어 있습니다. (하나만 연동 가능)");
        }

        SocialLink link = SocialLink.builder()
                .userId(userId)
                .provider(provider)
                .providerId(providerId)
                .createdAt(LocalDateTime.now())
                .build();
        socialLinkRepository.save(link);
    }

    public Long connectExisting(String loginId, String rawPassword,
            String provider, String providerId) {
        var user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        Long userId = user.getId();
        link(userId, provider, providerId);
        return userId;
    }

    public void connectAfterSignup(Long newUserId, String provider, String providerId) {
        link(newUserId, provider, providerId);
    }

    public record Pending(String provider, String providerId) {
    }
}