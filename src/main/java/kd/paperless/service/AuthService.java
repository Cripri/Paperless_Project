package kd.paperless.service;

import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    /** 평문 로그인: DB password_hash 컬럼에 평문이 들어 있다고 가정 */
    public Optional<User> authenticatePlain(String loginId, String rawPassword) {
        return userRepository.findByLoginIdAndPasswordHash(loginId, rawPassword);
    }

    // 변경 (BCrypt):
    // public Optional<User> suthenticateHash(String loginId, String hashPassword) {
    //     return userRepository.findByLoginId(loginId)
    //         .filter(u -> passwordEncoder.matches(hashPassword, u.getPasswordHash()));
    // }
}