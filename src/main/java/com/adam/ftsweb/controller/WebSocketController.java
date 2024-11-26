package com.adam.ftsweb.controller;

import com.adam.ftsweb.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.*;

@Component
@ServerEndpoint("/ws/{token}")
@Slf4j
public class WebSocketController {

    @Autowired
    private ObjectMapper objectMapper;
    private static UserService userService;
    private static Map<Long, Session> sessionMap = Collections.synchronizedMap(new HashMap<>());

    public static void setUserService(UserService userService) {
        WebSocketController.userService = userService;
    }

    @OnOpen
    public void onOpen(@PathParam("token")String token, Session session) {
        log.debug("onOpen");
        Assert.isTrue(StringUtils.isNotBlank(token), "ws connection token blank");
        Long ftsId = userService.getFtsIdByToken(token, false);
        Assert.notNull(ftsId, "ws connection invalid token");
        log.info("ws connection open token={} ftsId={} sessionId={}", token, ftsId, session.getId());
        sessionMap.put(ftsId, session);
    }

    @OnClose
    public void onClose(@PathParam("token")String token, Session session) {
        Assert.isTrue(StringUtils.isNotBlank(token), "ws connection token blank");
        Long ftsId = userService.getFtsIdByToken(token, false);
        Assert.notNull(ftsId, "ws connection invalid token");
        log.info("ws connection close token={} ftsId={} sessionId={}", token, ftsId, session.getId());
        sessionMap.remove(ftsId);
    }

    @OnMessage
    public void onMessage(@PathParam("token") String token, String message, Session session) {
        Assert.isTrue(StringUtils.isNotBlank(token), "ws connection token blank");
        Long ftsId = userService.getFtsIdByToken(token, false);
        Assert.notNull(ftsId, "ws connection invalid token");
        log.info("ws connection message token={} ftsId={} sessionId={} message={}", token, ftsId, session.getId(), message);
        session.getAsyncRemote().sendText("Hello!");
    }

    @OnError
    public void onError(@PathParam("token")String token, Session session, Throwable ex) {
        log.error("onError {}", userService, ex);
        Assert.isTrue(StringUtils.isNotBlank(token), "ws connection token blank");
        Long ftsId = userService.getFtsIdByToken(token, false);
        log.error("ws connection error token={} ftsId={} sessionId={}", token, ftsId, session.getId(), ex);
    }

}
