package kd.paperless.controller.get;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import kd.paperless.dto.ResidentRegistrationForm;

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

    @GetMapping("/residentregistration/form")
    public String apply(Model model) {
         // 화면 바인딩용 빈 객체
        ResidentRegistrationForm view = new ResidentRegistrationForm();
        // 필요 기본값
        view.setIncludeAll("ALL");
        model.addAttribute("form", view); // ← 이 한 줄이 핵심
        return "paperless/writer/form/residentregistration_form";
    }

    @GetMapping("/residentregistration/sign")
    public String signPopup() {
        return "paperless/writer/fragments/sign";
    }

}
