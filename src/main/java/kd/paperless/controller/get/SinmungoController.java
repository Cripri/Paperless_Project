package kd.paperless.controller.get;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import kd.paperless.entity.Sinmungo;
import kd.paperless.repository.SinmungoRepository;
import lombok.RequiredArgsConstructor;

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
        String dir = "desc";
        Sort.Direction direction = Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(direction, sort));

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String st = (status == null || status.isBlank()) ? null : status.trim();
        String ft = (searchType == null || searchType.isBlank()) ? "title" : searchType.trim();

        Page<Sinmungo> pageResult = sinmungoRepository.search(kw, st, ft, pageable);

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
        model.addAttribute("dir", dir);

        model.addAttribute("keyword", kw == null ? "" : kw);
        model.addAttribute("status", st == null ? "" : st);
        model.addAttribute("searchType", ft);
        return "sinmungo/sinmungo_list";
    }

    @GetMapping("/write")
    public String sinmnugo_submit() {
        return "sinmungo/sinmungo_write";
    }

    @PostMapping("/write")
    public String writeSubmit(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) Long writerId,
            @RequestParam(required = false) String telNum,
            @RequestParam(required = false) String noticeEmail,
            @RequestParam(required = false, defaultValue = "N") String noticeSms,
            @RequestParam(required = false) String postcode,
            @RequestParam(required = false) String addr1,
            @RequestParam(required = false) String addr2
            // TODO: 파일!
    ) {
        Sinmungo s = new Sinmungo();
        s.setTitle(title.trim());
        s.setContent(content);
        s.setWriterId(writerId == null ? 0L : writerId); // TODO: 사용자!
        s.setTelNum(telNum);
        s.setNoticeEmail((noticeEmail == null || noticeEmail.isBlank()) ? null : noticeEmail.trim());
        s.setNoticeSms(("Y".equalsIgnoreCase(noticeSms)) ? "Y" : "N");
        s.setPostcode(postcode);
        s.setAddr1(addr1);
        s.setAddr2(addr2);
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

        if (item.getViewCount() == null)
            item.setViewCount(0L);
        item.setViewCount(item.getViewCount() + 1);
        sinmungoRepository.save(item);

        String contentHtml = item.getContent() == null ? ""
                : org.springframework.web.util.HtmlUtils.htmlEscape(item.getContent())
                        .replace("\n", "<br>");

        String adminAnswerHtml = item.getAdminAnswer() == null ? ""
                : org.springframework.web.util.HtmlUtils.htmlEscape(item.getAdminAnswer())
                        .replace("\n", "<br>");

        model.addAttribute("it", item);
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
