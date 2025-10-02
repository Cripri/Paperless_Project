package kd.paperless.controller.post;

import kd.paperless.dto.PassportForm;
import kd.paperless.entity.PaperlessDoc;
import kd.paperless.repository.PaperlessDocRepository;
import kd.paperless.service.PassportApplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/passport")
public class PassportController {

    private final PassportApplyService passportApplyService;
    private final PaperlessDocRepository paperlessDocRepository;

    @GetMapping("/apply")
    public String applyForm(@ModelAttribute("passportForm") PassportForm passportForm) {
        // 초기값이 필요하면 여기서 세팅
        return "/paperless/writer/form/passport_apply";
    }


    @PostMapping("/preview")
    public String submitAndRedirect(@ModelAttribute("passportForm") PassportForm passportForm,
                                    BindingResult bindingResult,
                                    @SessionAttribute("loginUserId") Long userId) {

        if (passportForm.getKoreanName() == null || passportForm.getKoreanName().isBlank()) {
            bindingResult.rejectValue("koreanName", "required", "한글 성명을 입력하세요.");
        }
        if (passportForm.getPassportType() == null || passportForm.getPassportType().isBlank()) {
            bindingResult.rejectValue("passportType", "required", "여권 종류를 선택하세요.");
        }
        if (passportForm.getPageCount() == null) {
            bindingResult.rejectValue("pageCount", "required", "면수를 선택하세요.");
        }
        if (passportForm.getValidity() == null || passportForm.getValidity().isBlank()) {
            bindingResult.rejectValue("validity", "required", "기간을 선택하세요.");
        }
        if (bindingResult.hasErrors()) {
            return "/passport/passport_apply";
        }

        Long plId = passportApplyService.savePassportApplication(userId, passportForm);
        return "redirect:/passport/preview/" + plId;
    }

    /** plId 기반 프리뷰 페이지 */
    @GetMapping("/preview/{plId}")
    public String preview(@PathVariable Long plId, Model model) {
        PaperlessDoc doc = paperlessDocRepository.findById(plId)
                .orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다. plId=" + plId));

        model.addAttribute("plId", plId);
        model.addAttribute("doc", doc);

        // extra_json 편의 속성 꺼내기 (사진, 이름 등)
        Map<String, Object> extra = doc.getExtraJson();
        if (extra != null) {
            model.addAttribute("koreanName", extra.get("koreanName"));
            Object photo = extra.get("photo");
            if (photo instanceof Map) {
                Object url = ((Map<?, ?>) photo).get("downloadUrl");
                if (url != null) model.addAttribute("photoDownloadUrl", url.toString());
            }
        }
        return "/passport/passport_preview";
    }

    /* 예외 처리 */
    @ExceptionHandler(MultipartException.class)
    public String handleMultipartError(MultipartException ex, Model model) {
        model.addAttribute("error", "파일 업로드 중 오류: " + ex.getMessage());
        return "/passport/passport_apply";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(IllegalArgumentException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "/passport/passport_apply";
    }
}
