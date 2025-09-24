package kd.paperless.service;

import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;
import kd.paperless.request.FindIdRequest;
import kd.paperless.request.FindPwRequest;
import kd.paperless.request.ResetPwRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountFindService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<String> findLoginId(FindIdRequest req) {
        return userRepository.findByUserNameAndEmail(req.getName(), req.getEmail())
                .map(User::getLoginId);
    }

    /** 아이디+이메일 검증 (성공 시 사용자 반환) */
    public Optional<User> verifyUser(FindPwRequest req) {
        return userRepository.findByLoginIdAndEmail(req.getLoginId(), req.getEmail());
    }

     /** 특정 사용자 비밀번호 변경 — 해시(BCrypt) 저장 */
    @Transactional
    public boolean resetPassword(User user, ResetPwRequest req) {
        String raw = req.getNewPassword();
        if (raw == null || !raw.equals(req.getConfirmPassword())) {
            return false;
        }
        raw = raw.trim();

        // (선택) 이전 비밀번호와 동일한지 체크
        if (user.getPasswordHash() != null && passwordEncoder.matches(raw, user.getPasswordHash())) {
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(raw)); // ✅ 해시로 저장
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return true;
    }

    /** userId로 변경하는 버전 — 해시(BCrypt) 저장 */
    @Transactional
    public boolean resetPasswordForUserId(Long userId, ResetPwRequest req) {
        String raw = req.getNewPassword();
        String confirm = req.getConfirmPassword();
        if (raw == null || confirm == null) return false;

        final String pwd = raw.trim();        // ✅ 람다에서 쓸 final 변수
        final String confirmPwd = confirm.trim();
        if (!pwd.equals(confirmPwd)) return false;

        return userRepository.findById(userId).map(u -> {
            if (u.getPasswordHash() != null && passwordEncoder.matches(pwd, u.getPasswordHash())) {
                return false; // 이전 비번과 동일하면 실패 처리(선택)
            }
            u.setPasswordHash(passwordEncoder.encode(pwd)); // ✅ 해시 저장
            u.setUpdatedAt(LocalDateTime.now());
            userRepository.save(u);
            return true;
        }).orElse(false);
    }
}