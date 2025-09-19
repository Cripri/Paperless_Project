package kd.paperless.controller.get;

import kd.paperless.dto.notice.NoticeDetailDto;
import kd.paperless.dto.notice.NoticeListDto;
import kd.paperless.dto.notice.NoticePrevNextDto;
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

    Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "noticeId"));
    Page<NoticeListDto> result = noticeRepository.list(pageable)
        .map(NoticeListDto::from);

    model.addAttribute("items", result.getContent());
    model.addAttribute("totalCount", result.getTotalElements());
    model.addAttribute("currentPage", page);
    model.addAttribute("pageSize", size);
    model.addAttribute("totalPages", result.getTotalPages());

    int start = ((page - 1) / 10) * 10 + 1;
    int end = Math.min(start + 9, result.getTotalPages());
    model.addAttribute("startPage", start);
    model.addAttribute("endPage", end);

    model.addAttribute("status", "");

    return "notice/notice_list";
  }

  @GetMapping("/detail/{id}")
  public String detail(@PathVariable("id") Long id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size,
      Model model) {

    Notice entity = noticeRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("공지 없음: " + id));

    entity.setViewCount(entity.getViewCount() == null ? 1 : entity.getViewCount() + 1);
    noticeRepository.save(entity);

    NoticeDetailDto notice = NoticeDetailDto.from(entity);

    Long prevId = noticeRepository.findPrevId(id);
    Long nextId = noticeRepository.findNextId(id);

    NoticePrevNextDto prev = (prevId != null)
        ? noticeRepository.findById(prevId)
            .map(e -> new NoticePrevNextDto(e.getNoticeId(), e.getTitle()))
            .orElse(null)
        : null;

    NoticePrevNextDto next = (nextId != null)
        ? noticeRepository.findById(nextId)
            .map(e -> new NoticePrevNextDto(e.getNoticeId(), e.getTitle()))
            .orElse(null)
        : null;

    model.addAttribute("notice", notice);
    model.addAttribute("prev", prev);
    model.addAttribute("next", next);

    model.addAttribute("page", page);
    model.addAttribute("size", size);
    return "notice/notice_detail";
  }
}
