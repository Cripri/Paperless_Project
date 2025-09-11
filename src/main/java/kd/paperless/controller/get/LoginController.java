package kd.paperless.controller.get;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
    
    @GetMapping("/login")
    public String main() {
        return "/login/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "/login/signup";
    }

    @GetMapping("/findAccount")
    public String findAccount() {
        return "/login/findAccount";
    }
}
