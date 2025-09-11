package kd.paperless.controller.get;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class MainController {

    @GetMapping(value = {"/index", "/", "home"})
    public String home() {
        return "/main/main";
    }
}
