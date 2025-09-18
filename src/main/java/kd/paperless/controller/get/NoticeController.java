package kd.paperless.controller.get;

import kd.paperless.entity.Notice;
import kd.paperless.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

  private final NoticeRepository noticeRepository;

  @GetMapping("/list")
  public String list(@RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      Model model) {
    Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
    Page<Notice> result = noticeRepository.list(pageable);

    model.addAttribute("items", result.getContent());
    model.addAttribute("totalCount", result.getTotalElements());
    model.addAttribute("currentPage", page);
    model.addAttribute("pageSize", size);
    model.addAttribute("totalPages", result.getTotalPages());
    model.addAttribute("startPage", ((page - 1) / 10) * 10 + 1);
    model.addAttribute("endPage", Math.min(((page - 1) / 10) * 10 + 10, result.getTotalPages()));

    return "notice/notice_list";
  }

  @GetMapping("/detail/{id}")
  public String detail(@PathVariable("id") Long id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      Model model) {

    Notice notice = noticeRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("공지 없음: " + id));

    notice.setViewCount(notice.getViewCount() == null ? 1 : notice.getViewCount() + 1);
    noticeRepository.save(notice);

    Long prevId = noticeRepository.findPrevId(id);
    Long nextId = noticeRepository.findNextId(id);

    Notice prev = (prevId != null) ? noticeRepository.findById(prevId).orElse(null) : null;
    Notice next = (nextId != null) ? noticeRepository.findById(nextId).orElse(null) : null;

    model.addAttribute("prev", prev);
    model.addAttribute("next", next);

    model.addAttribute("notice", notice);
    model.addAttribute("page", page);
    model.addAttribute("size", size);

    return "notice/notice_detail";
  }
}
