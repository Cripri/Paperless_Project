package kd.paperless.controller.get;

import kd.paperless.entity.PaperlessDoc;
import kd.paperless.entity.Sinmungo;
import kd.paperless.entity.User;
import kd.paperless.repository.PaperlessDocRepository;
import kd.paperless.repository.SinmungoRepository;
import kd.paperless.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class MypageGetController {

    private final SinmungoRepository sinmungoRepository;
    private final PaperlessDocRepository paperlessDocRepository;
    private final UserRepository userRepository;

    @GetMapping("/mypage_myInfoEdit")
    public String mypage_myInfoEdit(@AuthenticationPrincipal(expression = "username") String loginId, Model model) {
        if (loginId == null)
            return "redirect:/login";
        User user = userRepository.findByLoginId(loginId).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("userId", user.getLoginId());
        model.addAttribute("userName", user.getUserName());
        return "mypage/mypage_myInfoEdit";
    }

    // ===== 신문고 목록 =====
    @GetMapping("/mypage_sinmungo")
    public String mypageSinmungo(
            @RequestParam(name = "q_searchTy", required = false, defaultValue = "1001") String qSearchTy,
            @RequestParam(name = "q_searchVal", required = false) String qSearchVal,
            @RequestParam(name = "q_currPage", required = false, defaultValue = "1") int qCurrPage,
            @RequestParam(name = "q_rowPerPage", required = false, defaultValue = "10") int qRowPerPage,
            @RequestParam(name = "status", required = false) String status, // 필요 시 UI에 추가해서 사용
            Model model) {
        // 0-base PageRequest
        int pageIndex = Math.max(qCurrPage - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, qRowPerPage, Sort.by(Sort.Direction.DESC, "smgId"));

        // searchType 매핑 (1001=title, 1002=content, 나머지=all)
        String searchType = switch (qSearchTy) {
            case "1001" -> "title";
            case "1002" -> "content";
            default -> "all";
        };

        String keyword = (qSearchVal == null || qSearchVal.isBlank()) ? null : qSearchVal.trim();
        String statusParam = (status == null || status.isBlank()) ? null : status.trim();

        Page<Sinmungo> page = sinmungoRepository.search(keyword, statusParam, searchType, pageable);

        model.addAttribute("page", page);
        model.addAttribute("items", page.getContent());
        model.addAttribute("totalCount", page.getTotalElements());

        // 폼 값 유지용
        model.addAttribute("q_searchTy", qSearchTy);
        model.addAttribute("q_searchVal", qSearchVal);
        model.addAttribute("q_rowPerPage", qRowPerPage);
        model.addAttribute("q_currPage", qCurrPage);
        model.addAttribute("status", status);

        return "mypage/mypage_sinmungo";
    }

    // ===== 간편서류 목록 =====
    @GetMapping("/mypage_simpleDoc")
    public String mypageSimpleDoc(
            @RequestParam(name = "q_currPage", required = false, defaultValue = "1") int qCurrPage,
            @RequestParam(name = "q_rowPerPage", required = false, defaultValue = "10") int qRowPerPage,
            Model model) {
        int pageIndex = Math.max(qCurrPage - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, qRowPerPage, Sort.by(Sort.Direction.DESC, "plId"));

        Page<PaperlessDoc> page = paperlessDocRepository.findAll(pageable);

        model.addAttribute("page", page);
        model.addAttribute("docs", page.getContent());
        model.addAttribute("totalCount", page.getTotalElements());

        model.addAttribute("q_rowPerPage", qRowPerPage);
        model.addAttribute("q_currPage", qCurrPage);

        return "mypage/mypage_simpleDoc";
    }
}