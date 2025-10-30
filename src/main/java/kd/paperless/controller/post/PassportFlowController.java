package kd.paperless.controller.post;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import kd.paperless.dto.PassportForm;
import kd.paperless.entity.Attachment;
import kd.paperless.entity.PaperlessDoc;
import kd.paperless.repository.AttachmentRepository;
import kd.paperless.repository.PaperlessDocRepository;
import kd.paperless.service.PassportPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@RequestMapping("/passport")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@SessionAttributes("passportForm")
public class PassportFlowController {

    private final PassportPdfService pdfService; // ← 요청대로 이 타입 사용
    private final PaperlessDocRepository docRepo;
    private final AttachmentRepository attachmentRepository;
    private final io.minio.MinioClient minioClient;

    @Value("${storage.minio.bucket}")
    private String bucket;

    @ModelAttribute("passportForm")
    public PassportForm passportForm() { return new PassportForm(); }

    // 1) 작성 폼
    @GetMapping("/apply")
    public String apply(Model model, @AuthenticationPrincipal(expression = "userId") Long userId) {
        if (userId == null) return "redirect:/login";
        if (!model.containsAttribute("passportForm")) model.addAttribute("passportForm", new PassportForm());
        return "paperless/writer/form/passport_apply";
    }

    @GetMapping("/form")
    public String form(Model model, @AuthenticationPrincipal(expression = "userId") Long userId) {
        return apply(model, userId);
    }

    // 2) 프리뷰 생성
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String preview(@Valid @ModelAttribute("passportForm") PassportForm form,
                          BindingResult binding,
                          Model model) {
        // 필요 검증
        if (binding.hasErrors()) return "paperless/writer/form/passport_apply";
        if ("TRAVEL_CERT".equalsIgnoreCase(form.getPassportType())
                && !StringUtils.hasText(form.getTravelMode())) {
            binding.rejectValue("travelMode", "required", "여행증명서 구분을 선택하세요.");
            return "paperless/writer/form/passport_apply";
        }
        if ("Y".equalsIgnoreCase(form.getDeliveryWanted())
                && (!StringUtils.hasText(form.getDeliveryPostcode()) || !StringUtils.hasText(form.getDeliveryAddress1()))) {
            binding.reject("delivery", "우편배송 주소를 입력하세요.");
            return "paperless/writer/form/passport_apply";
        }

        try {
            // ★ 존재하는 함수만 사용
            String fileId = pdfService.makePreview(form);
            model.addAttribute("fileId", fileId);
            model.addAttribute("passportForm", form);
            return "paperless/writer/form/passport_preview";
        } catch (Exception e) {
            binding.reject("previewFail", "여권 미리보기 생성 중 오류가 발생했습니다.");
            return "paperless/writer/form/passport_apply";
        }
    }

    // 프리뷰 페이지
    @GetMapping("/preview/{fileId}")
    public String previewPage(@PathVariable String fileId,
                              @ModelAttribute("passportForm") PassportForm form,
                              Model model) {
        model.addAttribute("fileId", fileId);
        return "paperless/writer/form/passport_preview";
    }

    // 프리뷰 PDF 스트리밍 (loadBytes만 사용)
    @GetMapping("/preview/file/{fileId}.pdf")
    public ResponseEntity<byte[]> previewFile(@PathVariable String fileId) throws Exception {
        byte[] bytes = pdfService.loadBytes(fileId);
        if (bytes == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"passport_preview.pdf\"")
                .body(bytes);
    }

    // 3) 제출
    @PostMapping("/submit")
    @Transactional
    public String submit(@ModelAttribute("passportForm") PassportForm form,
                         @RequestParam(value = "fileId", required = false) String fileId,
                         @AuthenticationPrincipal(expression = "userId") Long userId,
                         SessionStatus status,
                         Model model) {
        System.out.println("passport submit() fileId=" + fileId);
        try {
            if (userId == null) return "redirect:/login";

            // 1) 문서 저장
            PaperlessDoc entity = new PaperlessDoc();
            entity.setUserId(userId);
            entity.setDocType("PASSPORT");
            docRepo.save(entity);

            // 2) 업로드 (loadBytes → ByteArrayInputStream)
            if (StringUtils.hasText(fileId)) {
                byte[] bytes = pdfService.loadBytes(fileId);
                if (bytes == null || bytes.length == 0) {
                    throw new RuntimeException("프리뷰 파일이 없습니다: " + fileId);
                }

                String objectKey = "paperless/" + entity.getPlId() + "/passport_" + fileId + ".pdf";
                try (InputStream in = new ByteArrayInputStream(bytes)) {
                    minioClient.putObject(
                            io.minio.PutObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(objectKey)
                                    .contentType("application/pdf")
                                    .stream(in, bytes.length, -1)
                                    .build()
                    );
                    attachmentRepository.save(Attachment.builder()
                            .targetType("PAPERLESS_DOC")
                            .targetId(entity.getPlId())
                            .fileUri(objectKey)
                            .fileName("passport_application.pdf")
                            .mimeType("application/pdf")
                            .fileSize((long)bytes.length)
                            .build());
                }

                // 3) 프리뷰 정리 (getPreviewPath만 사용)
                Path previewPath = pdfService.getPreviewPath(fileId);
                Files.deleteIfExists(previewPath);
            }

            // 4) 세션 정리 + 완료 이동
            status.setComplete();
            model.addAttribute("plId", entity.getPlId());
            System.out.println("passport submit() OK fileId=" + fileId);
            return "redirect:/mypage_paperlessDoc";

        } catch (Exception e) {
            model.addAttribute("submitError", "제출 처리 중 오류가 발생했습니다.");
            if (fileId != null) model.addAttribute("fileId", fileId);
            return "paperless/writer/form/passport_preview";
        }
    }
}
