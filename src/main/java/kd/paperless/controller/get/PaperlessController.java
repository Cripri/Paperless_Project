package kd.paperless.controller.get;

import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/paperless/search")
    public String getMethodName() {
        return "paperless/search";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/residentregistration/sign")
    public String signPopup() {
        return "paperless/writer/fragments/sign";
    }

}
