package kd.paperless.controller.rest;

// import java.util.Map;

// import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// import kd.paperless.controller.service.UserService;
// import kd.paperless.dto.LoginRequest;
// import kd.paperless.dto.SignupRequest;
// import kd.paperless.dto.UserDto;
import lombok.RequiredArgsConstructor;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    // private final UserService service;

    // // 회원가입 - JSON Body로 받음
    // @PostMapping("/signup")
    // public Map<String, Object> signup(@RequestBody SignupRequest req) {
    //     Long id = service.signup(req);
    //     return Map.of("id", id);
    // }

    // // 로그인
    // @PostMapping("/login")
    // public UserDto login(@RequestBody LoginRequest req) {
    //     return service.login(req);
    // }

    // // 유저 조회
    // @GetMapping("/{loginId}")
    // public UserDto get(@PathVariable String loginId) {
    //     return service.findByLoginId(loginId);
    // }
}