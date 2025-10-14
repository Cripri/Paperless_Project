package kd.paperless.controller.get;

import kd.paperless.dto.ResidentRegistrationForm;
import kd.paperless.entity.PaperlessDoc;
import kd.paperless.entity.Sinmungo;
import kd.paperless.entity.User;
import kd.paperless.repository.PaperlessDocRepository;
import kd.paperless.repository.SinmungoRepository;
import kd.paperless.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

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
    public String mypageSinmungo(@AuthenticationPrincipal(expression = "username") String loginId,
            @RequestParam(name = "q_searchTy", defaultValue = "1001") String qSearchTy,
            @RequestParam(name = "q_searchVal", required = false) String qSearchVal,
            @RequestParam(name = "q_currPage", defaultValue = "1") int qCurrPage,
            @RequestParam(name = "q_rowPerPage", defaultValue = "10") int qRowPerPage,
            @RequestParam(name = "status", required = false) String status,
            Model model) {
        if (loginId == null)
            return "redirect:/login";
        User user = userRepository.findByLoginId(loginId).orElseThrow();

        int pageIndex = Math.max(qCurrPage - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, qRowPerPage, Sort.by(Sort.Direction.DESC, "smgId"));

        String searchType = switch (qSearchTy) {
            case "1001" -> "title";
            case "1002" -> "content";
            default -> "all";
        };
        String keyword = (qSearchVal == null || qSearchVal.isBlank()) ? null : qSearchVal.trim();
        String statusParam = (status == null || status.isBlank()) ? null : status.trim();

        Page<Sinmungo> page = sinmungoRepository
                .searchByUser(user.getId(), keyword, statusParam, searchType, pageable);

        model.addAttribute("page", page);
        model.addAttribute("items", page.getContent());
        model.addAttribute("totalCount", page.getTotalElements());
        model.addAttribute("q_searchTy", qSearchTy);
        model.addAttribute("q_searchVal", qSearchVal);
        model.addAttribute("q_rowPerPage", qRowPerPage);
        model.addAttribute("q_currPage", qCurrPage);
        model.addAttribute("status", status);

        return "mypage/mypage_sinmungo";
    }

    @GetMapping("mypage/mypage_sinmungo/detail/{id}")
    public String sinmungoDetail(@PathVariable Long id,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "searchType", required = false, defaultValue = "all") String searchType,
            Model model) {

        Sinmungo it = sinmungoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("민원을 찾을 수 없습니다."));

        // 이전/다음
        Long prevId = sinmungoRepository.findPrevId(id);
        Long nextId = sinmungoRepository.findNextId(id);
        Sinmungo prev = (prevId == null) ? null : sinmungoRepository.findById(prevId).orElse(null);
        Sinmungo next = (nextId == null) ? null : sinmungoRepository.findById(nextId).orElse(null);

        // 본문/답변: HTML escape + 줄바꿈 처리(안전하고 단순)
        String contentHtml = nl2brEscape(it.getContent());
        String adminAnswerHtml = nl2brEscape(it.getAdminAnswer());

        // 작성자 마스킹 (원하시는 규칙으로 교체 가능)
        String writerNameMasked = "김○○";

        // 상세 템플릿 모델
        model.addAttribute("it", it);
        model.addAttribute("prev", prev);
        model.addAttribute("next", next);
        model.addAttribute("contentHtml", contentHtml);
        model.addAttribute("adminAnswerHtml", adminAnswerHtml);
        model.addAttribute("files", null); // 첨부는 추후 연동
        model.addAttribute("writerNameMasked", writerNameMasked);

        // 목록 복귀용 파라미터 유지
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("searchType", searchType);

        return "sinmungo/sinmungo_detail";
    }

    // 안전한 줄바꿈 변환
    private String nl2brEscape(String src) {
        if (src == null || src.isBlank())
            return "";
        String escaped = HtmlUtils.htmlEscape(src);
        return escaped.replace("\n", "<br/>");
    }

    // ===== 간편서류 목록 =====
    @GetMapping("/mypage_paperlessDoc")
    public String mypagePaperlessDoc(
            @AuthenticationPrincipal(expression = "username") String loginId,
            @RequestParam(name = "q_status", required = false) String qStatus,
            @RequestParam(name = "q_currPage", defaultValue = "1") int qCurrPage,
            @RequestParam(name = "q_rowPerPage", defaultValue = "10") int qRowPerPage,
            Model model) {

        if (loginId == null)
            return "redirect:/login";

        User user = userRepository.findByLoginId(loginId).orElseThrow();

        int pageIndex = Math.max(qCurrPage - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, qRowPerPage, Sort.by(Sort.Direction.DESC, "plId"));

        Page<PaperlessDoc> page;

        // 문자열 → enum 변환 (유효하지 않으면 전체로 처리)
        PaperlessDoc.PaperlessStatus statusEnum = null;
        if (qStatus != null && !qStatus.isBlank()) {
            try {
                statusEnum = PaperlessDoc.PaperlessStatus.valueOf(qStatus.trim().toUpperCase());
            } catch (IllegalArgumentException ignore) {
                statusEnum = null;
            }
        }

        if (statusEnum == null) {
            page = paperlessDocRepository.findByUserId(user.getId(), pageable);
        } else {
            page = paperlessDocRepository.findByUserIdAndStatus(user.getId(), statusEnum, pageable);
        }

        model.addAttribute("page", page);
        model.addAttribute("docs", page.getContent());
        model.addAttribute("totalCount", page.getTotalElements());
        model.addAttribute("q_status", qStatus == null ? "" : qStatus);
        model.addAttribute("q_rowPerPage", qRowPerPage);
        model.addAttribute("q_currPage", qCurrPage);

        return "mypage/mypage_paperlessDoc";
    }

    @GetMapping("/mypage_paperlessDoc/{plId}")
    public String paperlessDocDetail(@PathVariable Long plId,
            @AuthenticationPrincipal(expression = "username") String loginId,
            Model model) {
        if (loginId == null)
            return "redirect:/login";

        User me = userRepository.findByLoginId(loginId).orElseThrow();
        PaperlessDoc doc = paperlessDocRepository.findById(plId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        // 소유자 보호
        if (!doc.getUserId().equals(me.getId())) {
            return "redirect:/mypage_paperlessDoc";
        }

        model.addAttribute("doc", doc); // 공통

        String type = doc.getDocType() == null ? "" : doc.getDocType().toUpperCase();
        switch (type) {
            case "RESIDENT":
            case "RESIDENT_REGISTRATION":
            case "RESIDENT_REGISTRATION_CERT":
            case "RESIDENT_REGISTRATION_COPY": {
                // ★ rrForm 바인딩 (extraJson -> DTO)
                ResidentRegistrationForm rrForm = toResidentDto(doc);
                model.addAttribute("rrForm", rrForm);
                return "paperless/writer/form/residentregistration_form";
                // 내가 신청한 파일로 가게 하고 싶은데 그건 나중에 물어봐서 해야겠담. 양식이랑 프리뷰밖에 없어서 못보는듯
                // 아니면 걍 내가 뭔가 잘못 햇을수도 있고 암튼암튼이다 ~
                // 암튼암튼 거머더라 어떻게하기로했지
                // 처리전에는 신청서류
                // 처리완료되면 등본입니당
            }
            case "PASSPORT": {
                // 여권 폼(ppForm)을 기대한다면 동일하게 만들어 넣어주세요.
                // PassportForm ppForm = toPassportDto(doc);
                // model.addAttribute("ppForm", ppForm);
                // 신청하고나서는 신청서류
                // 처리완료되면 암것도 안뜸 ㅇㅇ window alter 그냥 다됨 가서 찾으러가셈 ㅇㅇ
                return "paperless/writer/form/passport_form";
            }
            default: {
                ResidentRegistrationForm rrForm = toResidentDto(doc);
                model.addAttribute("rrForm", rrForm);
                return "paperless/writer/form/residentregistration_form";
            }
        }
    }

    private ResidentRegistrationForm toResidentDto(PaperlessDoc doc) {
        Map<String, Object> m = doc.getExtraJson();
        ResidentRegistrationForm f = ResidentRegistrationForm.defaultForGet();

        // 문서/동의 같은 상단 메타는 엔티티에서 동기화
        f.setDocType("resident_registration");
        f.setConsentYn(String.valueOf(doc.getConsentYn()));

        // 안전한 추출 헬퍼
        java.util.function.Function<String, String> S = k -> mapToString(m, k);

        // ① 신청인 기본정보
        f.setApplicantName(S.apply("applicantName"));
        f.setRrnFront(S.apply("rrnFront"));
        f.setRrnBack(S.apply("rrnBack"));
        f.setAddress1(S.apply("address1"));
        f.setAddress2(S.apply("address2"));
        f.setPhone(S.apply("phone"));

        // ② 옵션 필드들
        f.setFeeExempt(S.apply("feeExempt")); // "Y"/"N"
        f.setIncludeAll(S.apply("includeAll")); // "ALL"/"PART"
        f.setAddressHistoryMode(S.apply("addressHistoryMode")); // "ALL"/"RECENT"/"CUSTOM"
        f.setAddressHistoryYears(toInteger(m.get("addressHistoryYears")));
        f.setIncludeHouseholdReason(S.apply("includeHouseholdReason"));
        f.setIncludeHouseholdDate(S.apply("includeHouseholdDate"));
        f.setIncludeOccurReportDates(S.apply("includeOccurReportDates"));
        f.setChangeReasonScope(S.apply("changeReasonScope")); // "NONE"/"HOUSEHOLD"/"ALL_MEMBERS"
        f.setIncludeOtherNames(S.apply("includeOtherNames"));
        f.setRrnBackInclusion(S.apply("rrnBackInclusion")); // "NONE"/"SELF"/"HOUSEHOLD"
        f.setIncludeRelationshipToHead(S.apply("includeRelationshipToHead"));
        f.setIncludeCohabitants(S.apply("includeCohabitants"));

        // ③ 서명/기타
        f.setSignatureBase64(S.apply("signatureBase64"));

        // 필요시 전체 원본 JSON 문자열이 있으면 세팅(없으면 null 유지)
        f.setExtraJson(S.apply("extraJson"));

        return f;
    }

    private String mapToString(Map<String, Object> m, String key) {
        if (m == null)
            return null;
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private Integer toInteger(Object v) {
        if (v == null)
            return null;
        if (v instanceof Number n)
            return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
}