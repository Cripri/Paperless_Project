package kd.paperless.controller.file;

import io.minio.*;
import kd.paperless.entity.Attachment;
import kd.paperless.entity.PaperlessDoc;
import kd.paperless.repository.AttachmentRepository;
import kd.paperless.repository.PaperlessDocRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class AttachmentController {

  private final MinioClient minioClient;
  private final AttachmentRepository attachmentRepository;
  private final PaperlessDocRepository paperlessDocRepository;

  @Value("${storage.minio.bucket}")
  private String bucket;

  /** 단일 업로드 */
  @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Transactional
  public ResponseEntity<Attachment> upload(
      @RequestParam String targetType,
      @RequestParam Long targetId,
      @RequestPart("file") MultipartFile file) throws Exception {

    if (!StringUtils.hasText(targetType)) {
      return ResponseEntity.badRequest().build();
    }
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    String original = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
    String safeName = StringUtils.hasText(original) ? original : "file";
    String objectKey = buildObjectKey(safeName);

    try (InputStream in = file.getInputStream()) {
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(bucket)
              .object(objectKey)
              .contentType(file.getContentType())
              .stream(in, file.getSize(), -1)
              .build());
    }

    Attachment saved = attachmentRepository.save(
        Attachment.builder()
            .targetType(targetType.toUpperCase()) // ✅ DB에는 대문자로 통일
            .targetId(targetId)
            .fileUri(objectKey)
            .fileName(safeName)
            .mimeType(file.getContentType())
            .fileSize(file.getSize())
            .build());

    return ResponseEntity.ok(saved);
  }

  /** 여러 파일 업로드 */
  @PostMapping(path = "/upload-multi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Transactional
  public ResponseEntity<List<Attachment>> uploadMulti(
      @RequestParam String targetType,
      @RequestParam Long targetId,
      @RequestPart("files") List<MultipartFile> files) throws Exception {

    if (!StringUtils.hasText(targetType)) {
      return ResponseEntity.badRequest().build();
    }
    if (files == null || files.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    List<Attachment> result = new ArrayList<>();
    for (MultipartFile f : files) {
      if (f == null || f.isEmpty())
        continue;

      String original = Optional.ofNullable(f.getOriginalFilename()).orElse("file");
      String safeName = StringUtils.hasText(original) ? original : "file";
      String objectKey = buildObjectKey(safeName);

      try (InputStream in = f.getInputStream()) {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .contentType(f.getContentType())
                .stream(in, f.getSize(), -1)
                .build());
      }

      Attachment saved = attachmentRepository.save(
          Attachment.builder()
              .targetType(targetType.toUpperCase())
              .targetId(targetId)
              .fileUri(objectKey)
              .fileName(safeName)
              .mimeType(f.getContentType())
              .fileSize(f.getSize())
              .build());
      result.add(saved);
    }
    return ResponseEntity.ok(result);
  }

  /** 대상별 파일 목록 */
  @GetMapping("/list")
  public List<Attachment> list(
      @RequestParam String targetType,
      @RequestParam Long targetId) {
    return attachmentRepository.findByTargetTypeAndTargetIdOrderByFileIdAsc(
        targetType.toUpperCase(), targetId);
  }

  /** 다운로드 */
  @GetMapping("/{fileId}/download")
  public ResponseEntity<InputStreamResource> download(@PathVariable Long fileId) throws Exception {
    Attachment att = attachmentRepository.findById(fileId)
        .orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다: " + fileId));

    GetObjectArgs args = GetObjectArgs.builder()
        .bucket(bucket)
        .object(att.getFileUri())
        .build();
    InputStream in = minioClient.getObject(args);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(
        ContentDisposition.attachment()
            .filename(att.getFileName(), StandardCharsets.UTF_8)
            .build());
    MediaType ct = (att.getMimeType() != null)
        ? MediaType.parseMediaType(att.getMimeType())
        : MediaType.APPLICATION_OCTET_STREAM;
    headers.setContentType(ct);

    return ResponseEntity.ok()
        .headers(headers)
        .body(new InputStreamResource(in));
  }

  /** 삭제 */
  @DeleteMapping("/{fileId}")
  @Transactional
  public ResponseEntity<Void> delete(@PathVariable Long fileId) throws Exception {
    Attachment att = attachmentRepository.findById(fileId)
        .orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다: " + fileId));

    minioClient.removeObject(
        RemoveObjectArgs.builder()
            .bucket(bucket)
            .object(att.getFileUri())
            .build());
    attachmentRepository.delete(att);
    return ResponseEntity.noContent().build();
  }

  /** 객체 키 규칙: yyyy/MM/dd/uuid__원본파일명 */
  private static String buildObjectKey(String filename) {
    LocalDate d = LocalDate.now();
    return "%04d/%02d/%02d/%s__%s".formatted(
        d.getYear(), d.getMonthValue(), d.getDayOfMonth(), UUID.randomUUID(), filename);
  }

  @GetMapping("/inline/by-doc/{plId}")
  public ResponseEntity<InputStreamResource> inlineByDoc(
      @PathVariable Long plId,
      @AuthenticationPrincipal(expression = "userId") Long userId) throws Exception {

    // 1) 문서 존재/소유권 확인
    PaperlessDoc doc = paperlessDocRepository.findById(plId)
        .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));
    if (userId == null || !doc.getUserId().equals(userId)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // 2) 최신 첨부 1건 조회 (PAPERLESS_DOC)
    Attachment att = attachmentRepository
        .findTopByTargetTypeAndTargetIdOrderByFileIdDesc("PAPERLESS_DOC", plId)
        .orElseThrow(() -> new IllegalArgumentException("첨부 파일이 없습니다."));

    // 3) MinIO에서 스트림 열기
    GetObjectArgs args = GetObjectArgs.builder()
        .bucket(bucket)
        .object(att.getFileUri())
        .build();
    InputStream in = minioClient.getObject(args);

    // 4) Content-Type: PDF/이미지면 inline, 그 외는 다운로드
    MediaType ct = (att.getMimeType() != null)
        ? MediaType.parseMediaType(att.getMimeType())
        : MediaType.APPLICATION_OCTET_STREAM;

    boolean inlineOk = MediaType.APPLICATION_PDF.equals(ct) || ct.getType().equalsIgnoreCase("image");

    HttpHeaders headers = new HttpHeaders();
    ContentDisposition cd = inlineOk
        ? ContentDisposition.inline().filename(att.getFileName(), StandardCharsets.UTF_8).build()
        : ContentDisposition.attachment().filename(att.getFileName(), StandardCharsets.UTF_8).build();
    headers.setContentDisposition(cd);
    headers.setContentType(ct);

    return ResponseEntity.ok()
        .headers(headers)
        .body(new InputStreamResource(in));
  }
}