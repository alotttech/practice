package com.ews.demo.docscan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class DocSanController {
    @GetMapping("/")
    public String home() {
        return "docscan";
    }

    @GetMapping("/continue")
    public String nextPage(@RequestParam(name = "page", required = true, defaultValue = "docscan") String nextPage, Model model) {
        model.addAttribute("displayQR", false);
        model.addAttribute("phoneNo", "");
        model.addAttribute("message", "Please enter phone number to send the verification link");
        return nextPage;
    }
}
