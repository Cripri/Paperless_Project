package kd.paperless.controller.post;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import kd.paperless.dto.PassportForm;
import kd.paperless.entity.Attachment;
import kd.paperless.entity.PaperlessDoc;
import kd.paperless.repository.AttachmentRepository;
import kd.paperless.repository.PaperlessDocRepository;
import kd.paperless.service.PassportPdfOverlayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import java.io.IOException;
import java.nio.file.*;

@Controller
@RequiredArgsConstructor
public class PassportFlowController {

    private final PassportPdfOverlayService passportPdfService;
    private final PaperlessDocRepository docRepo;
    private final AttachmentRepository attachmentRepository;

    private final io.minio.MinioClient minioClient;

    @Value("${storage.minio.bucket}")
    private String bucket;

    private static final Path PHOTO_TMP = Paths.get(System.getProperty("java.io.tmpdir"), "passport_photo");

    /** 신청 화면 */
    @GetMapping("/passport/apply")
    public String apply(Model model) {
        if (!model.containsAttribute("passportForm")) {
            model.addAttribute("passportForm", new PassportForm());
        }
        return "paperless/writer/form/passport_apply";
    }

    /** 미리보기 생성 */
    @PostMapping(value = "/passport/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String preview(@Valid @ModelAttribute("passportForm") PassportForm form,
                          BindingResult binding,
                          Model model) {

        // 유효성
        if (binding.hasErrors()) {
            return "paperless/writer/form/passport_apply";
        }
        if ("TRAVEL_CERT".equals(form.getPassportType()) && !StringUtils.hasText(form.getTravelMode())) {
            binding.rejectValue("travelMode", "required", "여행증명서 구분을 선택하세요.");
            return "paperless/writer/form/passport_apply";
        }
        if ("Y".equals(form.getDeliveryWanted()) &&
                (!StringUtils.hasText(form.getDeliveryPostcode()) || !StringUtils.hasText(form.getDeliveryAddress1()))) {
            binding.reject("delivery", "우편배송 주소를 입력하세요.");
            return "paperless/writer/form/passport_apply";
        }

        // 사진 임시 저장
        Path photoPath = null;
        String photoUrl = null;
        try {
            if (form.getPhotoFile() != null && !form.getPhotoFile().isEmpty()) {
                Files.createDirectories(PHOTO_TMP);
                String fname = System.currentTimeMillis() + "_" + form.getPhotoFile().getOriginalFilename();
                photoPath = PHOTO_TMP.resolve(fname);
                form.getPhotoFile().transferTo(photoPath.toFile());
                photoUrl = "/passport/photo/" + fname;
            }
        } catch (IOException e) {
            binding.rejectValue("photoFile", "uploadFail", "사진 업로드에 실패했습니다.");
            return "paperless/writer/form/passport_apply";
        }

        // 미리보기 생성
        String fileId = passportPdfService.generatePreview(form, photoPath);

        // 모델
        model.addAttribute("fileId", fileId);
        model.addAttribute("passportForm", form);
        model.addAttribute("photoUrl", photoUrl);

        return "paperless/writer/form/passport_preview";
    }

    /** 프리뷰 PDF 스트리밍 */
    @GetMapping("/passport/preview/file/{fileId}.pdf")
    public @ResponseBody FileSystemResource previewPdf(@PathVariable String fileId, HttpServletResponse resp) throws IOException {
        Path path = passportPdfService.getPreviewPath(fileId);
        if (!Files.exists(path)) {
            resp.sendError(404);
            return null;
        }
        resp.setContentType(MediaType.APPLICATION_PDF_VALUE);
        return new FileSystemResource(path);
    }

    /** 임시 사진 서빙 */
    @GetMapping("/passport/photo/{filename}")
    public @ResponseBody FileSystemResource previewPhoto(@PathVariable String filename, HttpServletResponse resp) throws IOException {
        Path path = PHOTO_TMP.resolve(filename);
        if (!Files.exists(path)) {
            resp.sendError(404);
            return null;
        }
        String low = filename.toLowerCase();
        if (low.endsWith(".png")) resp.setContentType(MediaType.IMAGE_PNG_VALUE);
        else if (low.endsWith(".jpg") || low.endsWith(".jpeg")) resp.setContentType(MediaType.IMAGE_JPEG_VALUE);
        else resp.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return new FileSystemResource(path);
    }

    /** 제출 */
    @PostMapping("/passport/submit")
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
            docRepo.save(entity); // plId 생성

            // 2) 프리뷰 업로드 + 첨부 저장
            if (fileId != null && !fileId.isBlank()) {
                String objectKey = buildPassportObjectKey(entity.getPlId(), fileId);
                long size = passportPdfService.sizeOfPreview(fileId);
                try (java.io.InputStream in = passportPdfService.openPreviewStream(fileId)) {
                    minioClient.putObject(
                        io.minio.PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .contentType("application/pdf")
                            .stream(in, size, -1)
                            .build()
                    );

                    attachmentRepository.save(Attachment.builder()
                        .targetType("PAPERLESS_DOC")
                        .targetId(entity.getPlId())
                        .fileUri(objectKey)
                        .fileName("passport_application.pdf")
                        .mimeType("application/pdf")
                        .fileSize(size)
                        .build());
                } catch (Exception e) {
                    throw new RuntimeException("여권 PDF 업로드 실패", e);
                }
                // 3) 프리뷰 정리
                passportPdfService.cleanupPreview(fileId);
            }

            // 4) 세션 정리 + 리다이렉트
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

    private String buildPassportObjectKey(Long plId, String fileId) {
        return "paperless/" + plId + "/passport_" + fileId + ".pdf";
    }
}
