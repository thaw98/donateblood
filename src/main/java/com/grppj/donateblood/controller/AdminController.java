package com.grppj.donateblood.controller;


import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

@GetMapping("/admin")
public String index(Model model) {
 model.addAttribute("title", "Hello Spring Boot + Thymeleaf");
 model.addAttribute("now", LocalDateTime.now());
 return "redirect:admin"; 
}
}
