package com.smriti.url_shortener;
import org.springframework.web.bind.annotation.GetMapping;
import org. springframework.stereotype.Controller;
@Controller
public class HomeController {
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute(attributeName: "Title", attributeValue: "URL Shortener - thymeleaf");
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}   