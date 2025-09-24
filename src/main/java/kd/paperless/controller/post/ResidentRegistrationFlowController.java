package kd.paperless.controller.post;

import jakarta.validation.Valid;
import kd.paperless.dto.ResidentRegistrationForm;
import kd.paperless.entity.PaperlessDoc;
import kd.paperless.repository.PaperlessDocRepository;
import kd.paperless.service.ResidentRegistrationMapperService;
import kd.paperless.service.RrPdfOverlayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;  
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

@Controller
@RequestMapping("/residentregistration")
@RequiredArgsConstructor
@SessionAttributes("rrForm") // ✅ DTO를 프리뷰/제출까지 유지
public class ResidentRegistrationFlowController {

    private final RrPdfOverlayService pdfService;
    private final ResidentRegistrationMapperService mapper;
    private final PaperlessDocRepository docRepo;

    /** 세션에 DTO 없으면 새로 채움 */
    @ModelAttribute("rrForm")
    public ResidentRegistrationForm rrForm() {
        return ResidentRegistrationForm.defaultForGet();
    }

    /** 1) 작성 폼 */
    @GetMapping("/form")
    public String apply(@ModelAttribute("rrForm") ResidentRegistrationForm rrForm) {
        // rrForm은 세션에서 자동 바인딩
        return "paperless/writer/form/residentregistration_form";
    }

    /** 2) 프리뷰 생성 (DTO 갱신 → 세션 저장 → PDF 생성) */
    @PostMapping("/preview")
    public String makePreview(@Valid @ModelAttribute("rrForm") ResidentRegistrationForm rrForm,
                              BindingResult br,
                              Model model) throws Exception {
        if (br.hasErrors()) {
            return "paperless/writer/form/residentregistration_form";
        }
        // PDF 생성
        String fileId = pdfService.makePreview(rrForm);
        return "redirect:/residentregistration/preview/" + fileId;
    }

    /** 3) 프리뷰 화면 */
    @GetMapping("/preview/{fileId}")
    public String preview(@PathVariable String fileId, Model model,
                          @ModelAttribute("rrForm") ResidentRegistrationForm rrForm) {
        model.addAttribute("fileId", fileId);
        // rrForm은 세션에 유지 중 → 화면에서 값 확인 가능
        return "paperless/writer/form/residentregistration_preview";
    }

    /** 프리뷰 PDF 바이너리 서빙 (iframe에서 사용) */
    @GetMapping("/preview/file/{fileId}.pdf")
    public ResponseEntity<byte[]> previewFile(@PathVariable String fileId) throws Exception {
        byte[] data = pdfService.loadBytes(fileId);
        if (data == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    /** 4) 제출 → Entity 저장 */
    @PostMapping("/submit")
    public String submit(@ModelAttribute("rrForm") ResidentRegistrationForm rrForm,
                         SessionStatus status, Model model) {
        // DTO → Entity
        PaperlessDoc entity = mapper.toEntity(rrForm); // 요구한 고정값/extraJson 규칙 반영되어야 함
        docRepo.save(entity);

        // 세션 비우기
        status.setComplete();

        model.addAttribute("plId", entity.getPlId());
        return "paperless/writer/form/residentregistration_done";
    }
}
