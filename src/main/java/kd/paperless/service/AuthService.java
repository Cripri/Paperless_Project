package kd.paperless.service;

import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public Optional<User> authenticateHash(String loginId, String rawPassword) {
        return userRepository.findByLoginId(loginId)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()));
    }
}