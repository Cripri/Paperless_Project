package kd.paperless.controller.get;

import jakarta.servlet.http.HttpSession;
import kd.paperless.service.SnsLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/sns")
@RequiredArgsConstructor
public class SnsConnectGetController {

  private final SnsLinkService snsLinkService;

  @GetMapping("/connect")
  public String connectPage(HttpSession session, Model model) {
    var pending = snsLinkService.getPending(session)
        .orElse(null);
    if (pending == null) return "redirect:/login";

    model.addAttribute("provider", pending.provider());
    model.addAttribute("providerId", pending.providerId());
    return "login/snsConnect"; // 템플릿 경로에 맞춰 사용
  }
}