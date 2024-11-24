package com.adam.ftsweb.config;

import com.adam.ftsweb.constant.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.记住我实现
        for(Cookie cookie: request.getCookies()) {
            if(StringUtils.equals(cookie.getName(), SystemConstant.COOKIE_LOGIN_FTS_ID_KEY)) {
                String value = cookie.getValue();
                try {
                    long ftsId = Long.parseLong(value);
                    cookie.setMaxAge(SystemConstant.COOKIE_LOGIN_FTS_ID_MAX_AGE);
                    response.addCookie(cookie);
                    request.getSession().setAttribute(SystemConstant.SESSION_LOGIN_FTS_ID_KEY, ftsId);
                    log.debug("Logon user {} refreshing cookie max-age to {}", ftsId, SystemConstant.COOKIE_LOGIN_FTS_ID_MAX_AGE);
                    return true;
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
            return true;
        } else {
            if(object != null) {
                request.getSession().removeAttribute(SystemConstant.SESSION_LOGIN_FTS_ID_KEY);
                log.warn("Parsing session[key={},value={}] error, discarding", SystemConstant.SESSION_LOGIN_FTS_ID_KEY, object);
            }
            response.sendRedirect("/user/login");
            return false;
        }
    }

}
