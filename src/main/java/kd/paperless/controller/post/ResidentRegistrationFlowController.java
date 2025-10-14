package kd.paperless.controller.post;

import jakarta.validation.Valid;
import kd.paperless.dto.ResidentRegistrationForm;
import kd.paperless.entity.PaperlessDoc;
import kd.paperless.entity.User;
import kd.paperless.repository.PaperlessDocRepository;
import kd.paperless.service.ResidentRegistrationMapperService;
import kd.paperless.service.RrPdfOverlayService;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import kd.paperless.repository.UserRepository;

@Controller
@RequestMapping("/residentregistration")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@SessionAttributes("rrForm") // ✅ DTO를 프리뷰/제출까지 세션에 유지
public class ResidentRegistrationFlowController {

    private final RrPdfOverlayService pdfService;               // <-- makePreview / loadBytes / promoteToFinal 사용
    private final ResidentRegistrationMapperService mapper;     // DTO -> Entity 매핑
    private final PaperlessDocRepository docRepo;               // 저장
    private final UserRepository userRepository;

    /** 세션에 rrForm 없을 때 기본값 생성 */
    @ModelAttribute("rrForm")
    public ResidentRegistrationForm rrForm() {
        return ResidentRegistrationForm.defaultForGet();
    }

    // ==== 1) 작성 폼 진입 ====

    /** 새 주소 (/apply) */
    
    @GetMapping("/apply")
    public String apply(Model model, @AuthenticationPrincipal(expression = "userId") Long userId) {

        if(userId == null) return "redirect:/login";

        if (!model.containsAttribute("rrForm")) {
            model.addAttribute("rrForm", ResidentRegistrationForm.defaultForGet());
        }
        return "paperless/writer/form/residentregistration_form";
    }

    @GetMapping("/form")
    public String form(Model model, @AuthenticationPrincipal(expression = "userId") Long userId) {
        return apply(model,userId); // 동일하게 인증 주체로 위임
    }

    // ==== 2) 프리뷰 생성 ====

    /**
     * 폼 POST → 검증 → PDF 프리뷰 생성 → 프리뷰 페이지로 forward
     */
    @PostMapping("/preview")
    public String preview(
            @Valid @ModelAttribute("rrForm") ResidentRegistrationForm rrForm,
            BindingResult binding,
            Model model
    ) {
        // 조건부 검증: 주소변동사항이 RECENT면 년수 필수
        if ("RECENT".equalsIgnoreCase(rrForm.getAddressHistoryMode())
                && rrForm.getAddressHistoryYears() == null) {
            binding.rejectValue("addressHistoryYears", "NotNull", "최근 N년을 입력하세요.");
        }

        // 기본값 보정
        if (rrForm.getIncludeAll() == null) rrForm.setIncludeAll("ALL");
        if (rrForm.getFeeExempt() == null) rrForm.setFeeExempt("N");

        if (binding.hasErrors()) {
            // 에러 있으면 다시 폼
            return "paperless/writer/form/residentregistration_form";
        }

        try {
            // PDF 생성하고 fileId 획득
            String fileId = pdfService.makePreview(rrForm);
            model.addAttribute("fileId", fileId); // 프리뷰 템플릿에서 iframe에 사용
            return "paperless/writer/form/residentregistration_preview";
        } catch (Exception e) {
            binding.reject("previewFail", "미리보기 생성 중 오류가 발생했습니다.");
            return "paperless/writer/form/residentregistration_form";
        }
    }

    /**
     * 프리뷰 페이지(직접 URL 접근 허용)
     * - 템플릿에서 <iframe th:src="@{|/residentregistration/preview/file/${fileId}.pdf|}"/>
     */
    @GetMapping("/preview/{fileId}")
    public String previewPage(@PathVariable String fileId,
                              @ModelAttribute("rrForm") ResidentRegistrationForm rrForm,
                              Model model) {
        model.addAttribute("fileId", fileId);
        return "paperless/writer/form/residentregistration_preview";
    }

    /**
     * iframe에서 PDF 바이너리 로드
     */
    @GetMapping("/preview/file/{fileId}.pdf")
    public ResponseEntity<byte[]> previewFile(@PathVariable String fileId) throws Exception {
        byte[] bytes = pdfService.loadBytes(fileId);
        if (bytes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"")
                .body(bytes);
    }

    // ==== 3) 제출 ====
    /**
     * 프리뷰에서 "제출" 클릭 시
     * - hidden input name="fileId" 로 프리뷰 파일 id를 함께 전송하도록 프리뷰 템플릿 구성 필요
     */
    @PostMapping("/submit")
    public String submit(@ModelAttribute("rrForm") ResidentRegistrationForm rrForm,
                        @RequestParam(value = "fileId", required = false) String fileId,
                        SessionStatus status,
                        Model model,
                        @AuthenticationPrincipal(expression = "userId") Long userId) {
        try {
            // DB 저장
            PaperlessDoc entity = mapper.toEntity(rrForm);
            entity.setUserId(userId); // 👈 여기에 PK 저장
            docRepo.save(entity);

            if (fileId != null && !fileId.isBlank()) {
                pdfService.promoteToFinal(fileId);
            }

            status.setComplete();
            model.addAttribute("plId", entity.getPlId());
            return "redirect:/mypage_paperlessDoc";
        } catch (Exception e) {
            model.addAttribute("submitError", "제출 처리 중 오류가 발생했습니다.");
            if (fileId != null) model.addAttribute("fileId", fileId);
            return "paperless/writer/form/residentregistration_preview";
        }
    }

}
