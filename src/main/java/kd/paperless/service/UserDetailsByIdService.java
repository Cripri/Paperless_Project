package kd.paperless.service;

import kd.paperless.entity.User;
import kd.paperless.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsByIdService {

    private final UserRepository userRepository;

    public UserDetails loadUserById(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("회원을 찾을 수 없습니다. id=" + id));
        return new CustomUserDetails(u);
    }
}
