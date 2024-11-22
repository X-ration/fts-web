package com.adam.ftsweb.controller;

import com.adam.ftsweb.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
@Slf4j
public class UserController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/loginByFtsId")
    public String loginByFtsId(@RequestParam("ftsId") String ftsIdStr, @RequestParam String password, RedirectAttributes redirectAttributes) {
        log.debug("loginByFtsId ftsIdStr={},password={}", ftsIdStr, password);
        if(ftsIdStr == null || password == null) {
            redirectAttributes.addFlashAttribute("error", "无效的请求");
            return "redirect:/user/login";
        }
        int ftsId;
        try {
            ftsId = Integer.parseInt(ftsIdStr);
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "无效的输入");
            return "redirect:/user/login";
        }
        return "redirect:/index";
    }

    @PostMapping("/loginByEmail")
    public String loginByEmail(@RequestParam String email, @RequestParam String password, RedirectAttributes redirectAttributes) {
        log.debug("loginByEmail email={},password={}", email, password);
        if(email == null || password == null) {
            redirectAttributes.addFlashAttribute("error", "无效的请求");
            return "redirect:/user/login";
        }
        if(!StringUtil.isEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "无效的输入");
            return "redirect:/user/login";
        }
        return "redirect:/index";
    }

}
