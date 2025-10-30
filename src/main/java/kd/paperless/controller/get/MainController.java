package kd.paperless.controller.get;

import kd.paperless.entity.Notice;
import kd.paperless.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@RequiredArgsConstructor
@Controller
public class MainController {

    private final NoticeRepository noticeRepository;

    @GetMapping(value = {"/","home"})
    public String introNotices(Model model) {
        List<Notice> items = noticeRepository.findPostedTop(PageRequest.of(0, 3));
        model.addAttribute("items", items);
        return "main/intro";
    }   
    
}
