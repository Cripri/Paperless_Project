package kd.paperless.controller.get;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NoticeController {

  @GetMapping("/notice")
  public String notice() {
    return "/notice/notice";
  }

  @GetMapping("/notice_detail/{id}")
  public String notice_detail(@PathVariable String id) {
    return "/notice/notice_detail";
  }
}
