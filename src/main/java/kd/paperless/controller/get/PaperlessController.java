package kd.paperless.controller.get;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaperlessController {

    @GetMapping("/paperless/info")
    public String minwonMaker() {
        return "paperless/info";
    }

    @GetMapping("/paperless/search")
    public String getMethodName() {
        return "paperless/search";
    }

}
