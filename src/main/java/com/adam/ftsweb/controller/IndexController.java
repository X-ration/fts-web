package com.adam.ftsweb.controller;

import com.adam.ftsweb.constant.SystemConstant;
import com.adam.ftsweb.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    @Autowired
    private UserService userService;

    @GetMapping("")
    public String index(Model model) {
        Object object = request.getSession().getAttribute(SystemConstant.SESSION_LOGIN_FTS_TOKEN_KEY);
        if(object instanceof String) {
            String token = (String) object;
            Long ftsId = userService.getFtsIdByTokenAndRefresh(token);
            if(ftsId != null) {
                log.debug("indexPage ftsId={} token={}", ftsId, token);
                model.addAttribute("token", token);
                return "index";
            }
        }
        model.addAttribute("token", "invalid");
        return "index";
    }

}
