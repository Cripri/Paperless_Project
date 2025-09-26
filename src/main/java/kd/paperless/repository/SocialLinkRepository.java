package kd.paperless.repository;

import kd.paperless.entity.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {
    Optional<SocialLink> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByUserIdAndProvider(Long userId, String provider);
    boolean existsByUserId(Long userId);                // 한 유저당 1개 제한 체크
    Optional<SocialLink> findByUserId(Long userId);     // (선택) 필요 시 사용
}