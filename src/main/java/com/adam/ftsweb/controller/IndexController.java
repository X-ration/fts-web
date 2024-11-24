package com.adam.ftsweb.controller;

import com.adam.ftsweb.constant.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/index")
@Slf4j
public class IndexController {

    @Autowired
    private HttpServletRequest request;
    @Autowired
    private HttpServletResponse response;

    @GetMapping("")
    public String index(Model model) {
        Object object = request.getSession().getAttribute(SystemConstant.SESSION_LOGIN_FTS_ID_KEY);
        if(object instanceof Long) {
            long ftsId = (long) object;
            model.addAttribute("ftsId", ftsId);
            return "index";
        } else {
            model.addAttribute("ftsId", "invalid");
            return "index";
        }
    }

}
