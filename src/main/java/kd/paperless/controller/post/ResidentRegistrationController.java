package kd.paperless.controller.post;

import kd.paperless.dto.ResidentRegistrationForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@Controller 
@RequestMapping("/residentregistration")
public class ResidentRegistrationController {

    @GetMapping("/form")
    public String form(Model model){
        ResidentRegistrationForm form = new ResidentRegistrationForm();
        form.setIncludeAll("PART");
        form.setAddressHistoryMode("RECENT");
        form.setAddressHistoryYears(3);
        model.addAttribute("form", form);
        return "paperless/writer/form/residentregistration_form";
    }

    
    @GetMapping("/sign")
    public String sign(){
        return "paperless/writer/fragments/sign2";
    }

    @PostMapping("/apply")
    public String apply(@ModelAttribute("form") ResidentRegistrationForm form, Model model){
       
        if (form.getFeeExempt() != null && form.getFeeExempt() && (form.getFeeExemptReason() == null || form.getFeeExemptReason().isBlank())) {
            
            model.addAttribute("error", "수수료 면제 사유를 입력해 주세요.");
            return "paperless/writer/form/residentregistration_form";
        }

        
        if (form.getSignatureBase64() != null && form.getSignatureBase64().startsWith("data:image")) {
            byte[] pngBytes = decodeDataUrl(form.getSignatureBase64());
            
        }

        model.addAttribute("saved", true);
        return "paperless/writer/form/residentregistration_result";
    }

    private byte[] decodeDataUrl(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        if (comma < 0) throw new IllegalArgumentException("Invalid data URL");
        String base64 = dataUrl.substring(comma + 1);
        return Base64.getDecoder().decode(base64);
    }
}
