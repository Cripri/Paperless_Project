package kd.paperless.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kd.paperless.dto.UserDto;
import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;
import kd.paperless.request.LoginRequest;
import kd.paperless.request.SignupRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(SignupRequest req) {
        if (userRepo.existsByLoginId(req.getLoginId())) {
            throw new RuntimeException("이미 존재하는 아이디");
        }

        User u = new User();
        u.setLoginId(req.getLoginId());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setUserName(req.getUserName());
        u.setEmail(req.getEmail());
        u.setPhoneNum(req.getPhoneNum());
        u.setTelNum(req.getTelNum());
        u.setPostcode(req.getPostcode());
        u.setAddr1(req.getAddr1());
        u.setAddr2(req.getAddr2());
        u.setStatus("ACTIVE");
        u.setCreatedAt(LocalDateTime.now());

        return userRepo.save(u).getId();
    }

    // @Transactional(readOnly = true)
    // public UserDto login(LoginRequest req) {
    //     User u = userRepo.findByLoginId(req.getLoginId())
    //         .orElseThrow(() -> new RuntimeException("아이디 없음"));
    //     if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
    //         throw new RuntimeException("비밀번호 불일치");
    //     }
    //     return UserDto.from(u);
    // }

    // @Transactional(readOnly = true)
    // public UserDto findByLoginId(String loginId) {
    //     return userRepo.findByLoginId(loginId)
    //         .map(UserDto::from)
    //         .orElseThrow(() -> new RuntimeException("user not found"));
    // }
}