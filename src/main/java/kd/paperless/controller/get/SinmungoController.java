package kd.paperless.controller.get;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import kd.paperless.dto.sinmungo.SinmungoPrevNextDto;
import kd.paperless.dto.sinmungo.SinmungoDetailDto;
import kd.paperless.dto.sinmungo.SinmungoListDto;
import kd.paperless.dto.sinmungo.SinmungoWriteDto;
import kd.paperless.entity.Sinmungo;
import kd.paperless.repository.SinmungoRepository;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/sinmungo")
@RequiredArgsConstructor
public class SinmungoController {

    private final SinmungoRepository sinmungoRepository;

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

    @PostMapping("/write")
    public String writeSubmit(@Valid @ModelAttribute("dto") SinmungoWriteDto dto,
            BindingResult bindingResult,
            @RequestParam(required = false) Long writerId) {
        if (bindingResult.hasErrors()) {
            return "sinmungo/sinmungo_write";
        }
        dto.normalize();

        Sinmungo s = new Sinmungo();

        s.setTitle(dto.getTitle());
        s.setContent(dto.getContent());
        s.setWriterId(writerId == null ? 0L : writerId); // TODO 로그인 연동 시 교체
        s.setTelNum(dto.getTelNum());
        s.setNoticeEmail(dto.getNoticeEmail());
        s.setNoticeSms(("Y".equalsIgnoreCase(dto.getNoticeSms())) ? 'Y' : 'N');
        s.setPostcode(dto.getPostcode());
        s.setAddr1(dto.getAddr1());
        s.setAddr2(dto.getAddr2());
        s.setViewCount(0L);
        s.setStatus("접수");
        
        sinmungoRepository.save(s);
        return "redirect:/sinmungo/list";
    }

    @GetMapping("/detail/{smgId}")
    public String sinmungo_detail(
            @PathVariable("smgId") Long smgId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchType,
            Model model) {

        Sinmungo item = sinmungoRepository.findById(smgId)
                .orElseThrow(() -> new IllegalArgumentException("해당 글이 없습니다. smgId=" + smgId));

        item.setViewCount(item.getViewCount() == null ? 1L : item.getViewCount() + 1);
        sinmungoRepository.save(item);

        SinmungoDetailDto it = SinmungoDetailDto.from(item);

        String contentHtml = item.getContent() == null ? ""
                : org.springframework.web.util.HtmlUtils.htmlEscape(item.getContent()).replace("\n", "<br>");
        String adminAnswerHtml = item.getAdminAnswer() == null ? ""
                : org.springframework.web.util.HtmlUtils.htmlEscape(item.getAdminAnswer()).replace("\n", "<br>");

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

        model.addAttribute("prev", prev);
        model.addAttribute("next", next);

        model.addAttribute("it", it);
        model.addAttribute("contentHtml", contentHtml);
        model.addAttribute("adminAnswerHtml", adminAnswerHtml);

        model.addAttribute("page", page == null ? 1 : page);
        model.addAttribute("size", size == null ? 10 : size);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("searchType", (searchType == null || searchType.isBlank()) ? "title" : searchType);

        return "sinmungo/sinmungo_detail";
    }
}
