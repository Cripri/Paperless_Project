package kd.paperless.controller.get;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SinmungoController {

  @GetMapping("/sinmungo_guide")
  public String sinmungo_guide() {
    return "/sinmungo/sinmungo_guide";
  }

  @GetMapping("/sinmungo_list")
  public String sinmungo_list() {
    return "/sinmungo/sinmungo_list";
  }

  @GetMapping("/sinmungo_write")
  public String sinmnugo_submit() {
    return "/sinmungo/sinmungo_write";
  }

  @GetMapping("/sinmungo_detail/{id}")
  public String sinmungo_detail(@PathVariable("id") Integer id) {
    return "/sinmungo/sinmungo_detail";
  }

}
