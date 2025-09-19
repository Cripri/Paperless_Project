package kd.paperless.service;

import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;
import kd.paperless.request.FindIdRequest;
import kd.paperless.request.FindPwRequest;
import kd.paperless.request.ResetPwRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountFindService {

    private final UserRepository userRepository;

    public Optional<String> findLoginId(FindIdRequest req) {
        return userRepository.findByUserNameAndEmail(req.getName(), req.getEmail())
                .map(User::getLoginId);
    }

    /** 아이디+이메일 검증 (성공 시 사용자 반환) */
    public Optional<User> verifyUser(FindPwRequest req) {
        return userRepository.findByLoginIdAndEmail(req.getLoginId(), req.getEmail());
    }

    /** 특정 사용자 비밀번호 변경 — ★평문 저장★ */
    public boolean resetPassword(User user, ResetPwRequest req) {
        if (req.getNewPassword() == null
                || !req.getNewPassword().equals(req.getConfirmPassword())) {
            return false;
        }
        // password_hash 컬럼에 평문 그대로 저장
        user.setPasswordHash(req.getNewPassword());
        // BCrypt
        // user.setPasswordHash(passwordEncoder.encode(req.getNewPassword())); 
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return true;
    }

    /** userId로 변경하는 버전이 필요할 때 */
    public boolean resetPasswordForUserId(Long userId, ResetPwRequest req) {
        if (req.getNewPassword() == null
                || !req.getNewPassword().equals(req.getConfirmPassword())) {
            return false;
        }
        return userRepository.findById(userId).map(u -> {
            u.setPasswordHash(req.getNewPassword()); // ★평문 저장
            u.setUpdatedAt(LocalDateTime.now());
            userRepository.save(u);
            return true;
        }).orElse(false);
    }
}