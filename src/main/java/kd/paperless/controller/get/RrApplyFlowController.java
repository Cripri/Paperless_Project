package kd.paperless.controller.get;

import kd.paperless.dto.ResidentRegistrationForm;
import kd.paperless.service.RrPdfOverlayService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/residentregistration")
public class RrApplyFlowController {

    private final RrPdfOverlayService pdfService;

    /** 1) 작성 페이지 (GET) */
    @GetMapping("/apply")
    public String showForm(Model model) {
        model.addAttribute("form", ResidentRegistrationForm.defaultForGet());
        return "paperless/writer/form/residentregistration_form"; // 네가 쓰던 템플릿 이름
    }

    /** 2) 미리보기 생성 (POST) - 입력값으로 PDF 오버레이 생성 */
    @PostMapping("/preview")
    public String makePreview(@ModelAttribute ResidentRegistrationForm form) throws Exception {
        String fileId = pdfService.makePreview(form); // temp에 저장
        return "redirect:/residentregistration/preview/" + fileId;
    }

    /** 3) 미리보기 페이지 (GET) - iframe으로 PDF 보기 + 제출/출력 버튼 */
    @GetMapping("/preview/{id}")
    public String previewPage(@PathVariable String id, Model model) {
        model.addAttribute("fileId", id);
        return "paperless/writer/form/rr_preview";
    }

    /** 4) PDF 스트리밍 (GET) - 브라우저 내장 뷰어 */
    @GetMapping("/pdf/{id}")
    public ResponseEntity<ByteArrayResource> streamPdf(@PathVariable String id) throws Exception {
        byte[] data = pdfService.loadBytes(id);
        if (data == null) return ResponseEntity.notFound().build();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_PDF);
        h.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resident_registration_preview.pdf\"");
        return new ResponseEntity<>(new ByteArrayResource(data), h, HttpStatus.OK);
    }

    /** 5) 제출 (POST) - 미리보기 파일 영구보관 + DB 메타 저장 후 마이페이지로 */
    @PostMapping("/submit")
    public String submit(@RequestParam String fileId) throws Exception {
        pdfService.promoteToFinal(fileId);   // temp → final 이동/복사
        // TODO: DB에 신청 레코드 저장(사용자, fileId, 요약정보 등)
        return "redirect:/mypage";           // 마이페이지 URL로 리다이렉트
    }

    /** 6) 인쇄 버튼은 그냥 /pdf/{id}를 새창으로 열게 했으니 별도 컨트롤러 필요 없음 */
}
