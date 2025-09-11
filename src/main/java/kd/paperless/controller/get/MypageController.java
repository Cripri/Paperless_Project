package kd.paperless.controller.get;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MypageController {

    @GetMapping("/mypage_sinmungo")
    public String mypage_sinmungo() {
        return "/mypage/mypage_sinmungo";
    }

    @GetMapping("/mypage_simpleDoc")
    public String mypage_simpleDoc() {
        return "/mypage/mypage_simpleDoc";
    }

    @GetMapping("/mypage_myInfoEdit")
    public String mypage_myInfoEdit() {
        return "/mypage/mypage_myInfoEdit";
    }
}
