package kd.paperless.controller.get;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import kd.paperless.dto.sinmungo.SinmungoDetailDto;
import kd.paperless.dto.sinmungo.SinmungoListDto;
import kd.paperless.dto.sinmungo.SinmungoPrevNextDto;
import kd.paperless.dto.sinmungo.SinmungoWriteDto;
import kd.paperless.entity.Attachment;
import kd.paperless.entity.Sinmungo;
import kd.paperless.entity.User;
import kd.paperless.repository.AttachmentRepository;
import kd.paperless.repository.SinmungoRepository;
import kd.paperless.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/sinmungo")
@RequiredArgsConstructor
public class SinmungoController {

  private final SinmungoRepository sinmungoRepository;
  private final AttachmentRepository attachmentRepository;
  private final UserRepository userRepository;
  private final MinioClient minioClient;

  @Value("${storage.minio.bucket}")
  private String bucket;

  @GetMapping("/guide")
  public String sinmungo_guide() {
    return "/sinmungo/sinmungo_guide";
  }

  @GetMapping("/list")
  public String sinmungo_list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,  
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false, name = "searchType") String searchType,
      Model model) {

    String sort = "createdAt";
    Sort.Direction direction = Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(direction, sort));

    String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    String st = (status == null || status.isBlank()) ? null : status.trim();
    String ft = (searchType == null || searchType.isBlank()) ? "title" : searchType.trim();

    Page<SinmungoListDto> pageResult = sinmungoRepository.search(kw, st, ft, pageable)
        .map(SinmungoListDto::from);

    List<Long> writerIds = pageResult.getContent().stream()
        .map(SinmungoListDto::getWriterId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    Map<Long, String> writerNames = new HashMap<>();
    if (!writerIds.isEmpty()) {
      List<User> users = userRepository.findAllByIdIn(writerIds);
      for (User u : users) {
        writerNames.put(u.getId(), maskName(u.getUserName()));
      }
    }
    model.addAttribute("writerNames", writerNames);
    long total = pageResult.getTotalElements();
    int totalPages = pageResult.getTotalPages();

    int blockSize = 10;
    int start = ((page - 1) / blockSize) * blockSize + 1;
    int end = Math.min(start + blockSize - 1, Math.max(totalPages, 1));

    model.addAttribute("items", pageResult.getContent());
    model.addAttribute("totalCount", total);

    model.addAttribute("currentPage", page);
    model.addAttribute("pageSize", size);
    model.addAttribute("totalPages", totalPages);
    model.addAttribute("startPage", start);
    model.addAttribute("endPage", end);
    model.addAttribute("sort", sort);
    model.addAttribute("dir", "desc");

    model.addAttribute("keyword", kw == null ? "" : kw);
    model.addAttribute("status", st == null ? "" : st);
    model.addAttribute("searchType", ft);

    return "sinmungo/sinmungo_list";
  }

  @GetMapping("/write")
  public String sinmungo_submit(Model model) {
    return "sinmungo/sinmungo_write";
  }

  @PostMapping(value = "/write", consumes = "multipart/form-data")
  @Transactional
  public String writeSubmit(
      @AuthenticationPrincipal(expression = "userId") Long writerId,
      @Valid @ModelAttribute("dto") SinmungoWriteDto dto,
      BindingResult bindingResult,
      @RequestParam(name = "files", required = false) List<MultipartFile> files) {

    if (bindingResult.hasErrors()) {
      return "sinmungo/sinmungo_write";
    }
    if (writerId == null) {
      return "redirect:/login";
    }

    dto.normalize();

    // 1) 본문 저장
    Sinmungo s = new Sinmungo();
    s.setTitle(dto.getTitle());
    s.setContent(dto.getContent());
    s.setWriterId(writerId);
    s.setTelNum(dto.getTelNum());
    s.setNoticeEmail(dto.getNoticeEmail());
    s.setNoticeSms(("Y".equalsIgnoreCase(dto.getNoticeSms())) ? 'Y' : 'N');
    s.setPostcode(dto.getPostcode());
    s.setAddr1(dto.getAddr1());
    s.setAddr2(dto.getAddr2());
    s.setViewCount(0L);
    s.setStatus("접수");

    Sinmungo saved = sinmungoRepository.save(s);
    Long smgId = saved.getSmgId();

    // 2) 첨부 업로드(있으면)
    if (files != null) {
      for (MultipartFile f : files) {
        if (f == null || f.isEmpty())
          continue;

        String original = Optional.ofNullable(f.getOriginalFilename()).orElse("file");
        String safeName = original.isBlank() ? "file" : original;
        String contentType = (f.getContentType() != null) ? f.getContentType()
            : "application/octet-stream";
        String objectKey = buildObjectKey(safeName);

        try (InputStream in = f.getInputStream()) {
          minioClient.putObject(
              PutObjectArgs.builder()
                  .bucket(bucket)
                  .object(objectKey)
                  .contentType(contentType)
                  .stream(in, f.getSize(), -1)
                  .build());
        } catch (Exception e) {
          throw new RuntimeException("파일 업로드 실패: " + safeName, e);
        }

        attachmentRepository.save(
            Attachment.builder()
                .targetType("SINMUNGO")
                .targetId(smgId)
                .fileUri(objectKey)
                .fileName(safeName)
                .mimeType(contentType)
                .fileSize(f.getSize())
                .build());
      }
    }

    return "redirect:/sinmungo/list";
  }

  @GetMapping("/detail/{smgId}")
  @Transactional
  public String sinmungo_detail(
      @PathVariable("smgId") Long smgId,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String searchType,
      Model model) {

    Sinmungo item = sinmungoRepository.findByIdWithAdmin(smgId)
        .orElseThrow(() -> new IllegalArgumentException("해당 글이 없습니다. smgId=" + smgId));

    // 조회수 증가
    item.setViewCount(item.getViewCount() == null ? 1L : item.getViewCount() + 1);
    sinmungoRepository.save(item);

    SinmungoDetailDto it = SinmungoDetailDto.from(item);

    String contentHtml = item.getContent() == null ? ""
        : org.springframework.web.util.HtmlUtils.htmlEscape(item.getContent()).replace("\n",
            "<br>");
    String adminAnswerHtml = item.getAdminAnswer() == null ? ""
        : org.springframework.web.util.HtmlUtils.htmlEscape(item.getAdminAnswer()).replace("\n",
            "<br>");

    Long prevId = sinmungoRepository.findPrevId(smgId);
    Long nextId = sinmungoRepository.findNextId(smgId);

    SinmungoPrevNextDto prev = (prevId != null)
        ? sinmungoRepository.findById(prevId)
            .map(e -> new SinmungoPrevNextDto(e.getSmgId(), e.getTitle()))
            .orElse(null)
        : null;

    SinmungoPrevNextDto next = (nextId != null)
        ? sinmungoRepository.findById(nextId)
            .map(e -> new SinmungoPrevNextDto(e.getSmgId(), e.getTitle()))
            .orElse(null)
        : null;

    List<Attachment> attachments = attachmentRepository
        .findByTargetTypeAndTargetIdOrderByFileIdAsc("SINMUNGO", smgId);
    List<FileView> files = new ArrayList<>();
    for (Attachment a : attachments) {
      files.add(new FileView(
          a.getFileId(),
          a.getFileName(),
          a.getFileSize(),
          "/files/" + a.getFileId() + "/download"));
    }

    String writerNameMasked = "-";
    if (item.getWriterId() != null) {
      writerNameMasked = userRepository.findById(item.getWriterId())
          .map(u -> maskName(u.getUserName()))
          .orElse("-");
    }
    model.addAttribute("writerNameMasked", writerNameMasked);

    String adminNameMasked = (it.getAdminName() != null && !it.getAdminName().isBlank())
        ? maskName(it.getAdminName())
        : "-";
    model.addAttribute("adminNameMasked", adminNameMasked);

    model.addAttribute("prev", prev);
    model.addAttribute("next", next);
    model.addAttribute("it", it);
    model.addAttribute("contentHtml", contentHtml);
    model.addAttribute("adminAnswerHtml", adminAnswerHtml);
    model.addAttribute("files", files);

    model.addAttribute("page", page == null ? 1 : page);
    model.addAttribute("size", size == null ? 10 : size);
    model.addAttribute("keyword", keyword == null ? "" : keyword);
    model.addAttribute("status", status == null ? "" : status);
    model.addAttribute("searchType", (searchType == null || searchType.isBlank()) ? "title" : searchType);

    return "sinmungo/sinmungo_detail";
  }

  private static String maskName(String name) {
    if (name == null || name.isBlank())
      return "-";
    String trimmed = name.trim();
    if (trimmed.length() == 1)
      return trimmed.charAt(0) + "○";
    return trimmed.charAt(0) + "OO";
  }

  @Getter
  @AllArgsConstructor
  public static class FileView {
    private Long fileId;
    private String filename;
    private Long size;
    private String url;
  }

  private static String buildObjectKey(String filename) {
    LocalDate d = LocalDate.now();
    return "%04d/%02d/%02d/%s__%s".formatted(
        d.getYear(), d.getMonthValue(), d.getDayOfMonth(), UUID.randomUUID(), filename);
  }
}
