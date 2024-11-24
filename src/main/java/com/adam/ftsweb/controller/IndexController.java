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
        //1.记住我实现
        for(Cookie cookie: request.getCookies()) {
            if(StringUtils.equals(cookie.getName(), SystemConstant.COOKIE_LOGIN_FTS_ID_KEY)) {
                String value = cookie.getValue();
                try {
                    long ftsId = Long.parseLong(value);
                    model.addAttribute("ftsId", ftsId);
                    cookie.setMaxAge(SystemConstant.COOKIE_LOGIN_FTS_ID_MAX_AGE);
                    response.addCookie(cookie);
                    log.info("Logon user refreshing cookie max-age to {}", SystemConstant.COOKIE_LOGIN_FTS_ID_MAX_AGE);
                    return "index";
                } catch (NumberFormatException e) {
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    log.warn("Parsing cookie[key={},value={}] error, discarding", cookie.getName(), value);
                }
                break;
            }
        }
        //2.默认存储于会话(session)级别
        Object object = request.getSession().getAttribute(SystemConstant.SESSION_LOGIN_FTS_ID_KEY);
        if(object instanceof Long) {
            long ftsId = (long) object;
            model.addAttribute("ftsId", ftsId);
            return "index";
        } else {
            if(object != null) {
                request.getSession().removeAttribute(SystemConstant.SESSION_LOGIN_FTS_ID_KEY);
                log.warn("Parsing session[key={},value={}] error, discarding", SystemConstant.SESSION_LOGIN_FTS_ID_KEY, object);
            }
            return "redirect:/user/login";
        }
    }

}
