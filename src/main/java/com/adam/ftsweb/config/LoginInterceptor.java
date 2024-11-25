package com.adam.ftsweb.config;

import com.adam.ftsweb.constant.SystemConstant;
import com.adam.ftsweb.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.记住我实现
        if(request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (StringUtils.equals(cookie.getName(), SystemConstant.COOKIE_LOGIN_FTS_TOKEN_KEY)) {
                    String value = cookie.getValue();
                    Long ftsId = userService.getFtsIdByTokenAndRefresh(value);
                    if (ftsId != null) {
                        cookie.setMaxAge(SystemConstant.COOKIE_LOGIN_FTS_TOKEN_MAX_AGE);
                        response.addCookie(cookie);
                        request.getSession().setAttribute(SystemConstant.SESSION_LOGIN_FTS_TOKEN_KEY, value);
                        return true;
                    } else {
                        cookie.setMaxAge(0);
                        response.addCookie(cookie);
                        log.warn("Bad cookie [key={},value={}] ,discarding", cookie.getName(), value);
                    }
                    break;
                }
            }
        }
        //2.默认存储于会话(session)级别
        Object object = request.getSession().getAttribute(SystemConstant.SESSION_LOGIN_FTS_TOKEN_KEY);
        if(object instanceof String) {
            Long ftsId = userService.getFtsIdByTokenAndRefresh((String) object);
            if(ftsId != null) {
                return true;
            }
        } else {
            if(object != null) {
                request.getSession().removeAttribute(SystemConstant.SESSION_LOGIN_FTS_TOKEN_KEY);
                log.warn("Parsing session[key={},value={}] error, discarding", SystemConstant.SESSION_LOGIN_FTS_TOKEN_KEY, object);
            }
        }
        response.sendRedirect("/user/login");
        return false;
    }

}
