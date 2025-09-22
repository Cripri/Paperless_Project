package kd.paperless.controller.get;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginGetController {

    @GetMapping("/login")
    public String main() {
        return "login/login";
    }

    @GetMapping("/findAccount")
    public String findAccount() {
        return "login/findAccount";
    }

    @GetMapping("/snsConnect")
    public String snsConnect() {
        return "login/snsConnect";
    }   
}