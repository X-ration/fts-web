package com.adam.ftsweb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/index")
public class IndexController {

    @RequestMapping("")
    public String index(@RequestParam int ftsId, Model model) {
        model.addAttribute("ftsId", ftsId);
        return "index";
    }

}
