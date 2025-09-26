package kd.paperless.controller.rest;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/sns")
public class SnsConnectController {

  @GetMapping("/connect")
  public String connectPage(HttpSession session, Model model) {
    String provider   = (String) session.getAttribute("PENDING_PROVIDER");
    String providerId = (String) session.getAttribute("PENDING_PROVIDER_ID");
    if (provider == null || providerId == null) return "redirect:/login";
    model.addAttribute("provider", provider);
    model.addAttribute("providerId", providerId);
    return "login/snsConnect"; // 템플릿 경로에 맞춰 조정
  }
}
