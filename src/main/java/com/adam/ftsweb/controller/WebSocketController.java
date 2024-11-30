package com.adam.ftsweb.controller;

import com.adam.ftsweb.config.WebConfig;
import com.adam.ftsweb.constant.WebSocketConstant;
import com.adam.ftsweb.dto.WebSocketDTO;
import com.adam.ftsweb.dto.WebSocketLeftMessage;
import com.adam.ftsweb.dto.WebSocketMainMessage;
import com.adam.ftsweb.dto.WebSocketResponseDTO;
import com.adam.ftsweb.po.Message;
import com.adam.ftsweb.service.UserService;
import com.adam.ftsweb.util.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.time.LocalDateTime;
import java.util.*;

@Component
@ServerEndpoint("/ws/{token}")
@Slf4j
public class WebSocketController {

    private static UserService userService;
    private static ObjectMapper objectMapper;
    private static Map<Long, Session> sessionMap = Collections.synchronizedMap(new HashMap<>());

    public static void setUserService(UserService userService, ObjectMapper objectMapper) {
        WebSocketController.userService = userService;
        WebSocketController.objectMapper = objectMapper;
    }

    @OnOpen
    public void onOpen(@PathParam("token")String token, Session session) {
        log.debug("onOpen");
        Assert.isTrue(StringUtils.isNotBlank(token), "ws connection token blank");
        Long ftsId = userService.getFtsIdByToken(token, false);
        Assert.notNull(ftsId, "ws connection invalid token");
        log.info("ws connection open token={} ftsId={} sessionId={}", token, ftsId, session.getId());
        sessionMap.put(ftsId, session);
        initializeMessageList(ftsId, session);
    }

    private void initializeMessageList(long ftsId, Session session) {
        WebSocketDTO webSocketDTO = new WebSocketDTO();
        webSocketDTO.setType(WebSocketDTO.WebSocketDTOType.INITIAL_MESSAGE_LIST);
        List<WebSocketLeftMessage> messageList = userService.queryMessageListByFtsId(ftsId);
        webSocketDTO.setData(messageList);
        try {
            String json = objectMapper.writeValueAsString(webSocketDTO);
            session.getAsyncRemote().sendText(json);
        } catch (JsonProcessingException e) {
            log.error("initializeMessageList writeValueAsString error", e);
        }
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
    public void onMessage(@PathParam("token") String token, String message, Session session) throws JsonProcessingException {
        Assert.isTrue(StringUtils.isNotBlank(token), "ws connection token blank");
        Long ftsId = userService.getFtsIdByToken(token, false);
        Assert.notNull(ftsId, "ws connection invalid token");
        log.info("ws connection message token={} ftsId={} sessionId={} message={}", token, ftsId, session.getId(), message);
        WebSocketDTO requestDTO = objectMapper.readValue(message, new TypeReference<WebSocketDTO>() {});
        WebSocketResponseDTO responseDTO = new WebSocketResponseDTO();
        switch (requestDTO.getType()) {
            case ADD_FRIEND:
                addFriend(requestDTO, responseDTO, ftsId);
                break;
            case RETRIEVE_MESSAGE_LIST:
                retrieveMessageList(requestDTO, responseDTO, ftsId);
                break;
            case SEND_MESSAGE_TEXT:
                sendMessageText(requestDTO, responseDTO, ftsId);
                break;
            default:
                responseDTO.setType(WebSocketDTO.WebSocketDTOType.NOT_RESOLVABLE);
        }
        try {
            String responseJson = objectMapper.writeValueAsString(responseDTO);
            session.getAsyncRemote().sendText(responseJson);
        } catch (JsonProcessingException e) {
            log.error("ObjectMapper write responseJson error,dto={}", responseDTO, e);
        }
    }

    private void sendMessageText(WebSocketDTO requestDTO, WebSocketResponseDTO responseDTO, long ftsId) {
        responseDTO.setType(WebSocketDTO.WebSocketDTOType.SEND_MESSAGE_TEXT_RESULT);
        Object data = requestDTO.getData();
        if(data == null) {
            responseDTO.setSuccess(false);
            responseDTO.setMessage(WebSocketConstant.INVALID_PARAM);
        } else {
            try {
                Map<String,Object> dataMap = (Map<String,Object>) data;
                long toFtsId = Long.parseLong(String.valueOf(dataMap.get("toFtsId")));
                String messageText = String.valueOf(dataMap.get("text"));
                Response<?> sendMessageResponse = userService.sendMessage(ftsId, toFtsId, messageText, Message.MessageType.text, null);
                responseDTO.setSuccess(sendMessageResponse.isSuccess());
                if(!sendMessageResponse.isSuccess()) {
                    responseDTO.setMessage(sendMessageResponse.getMessage());
                } else {
                    String fromNickname = (String) sendMessageResponse.getData();
                    //push message
                    WebSocketDTO pushMessageDTO  = new WebSocketDTO();
                    pushMessageDTO.setType(WebSocketDTO.WebSocketDTOType.MESSAGE);
                    WebSocketMainMessage mainMessage = new WebSocketMainMessage();
                    mainMessage.setType(Message.MessageType.text);
                    mainMessage.setText(messageText);
                    mainMessage.setFromFtsId(ftsId);
                    mainMessage.setToFtsId(toFtsId);
                    mainMessage.setCreateTime(LocalDateTime.now().format(WebConfig.DATE_TIME_FORMATTER));
                    mainMessage.setFromNickname(fromNickname);
                    pushMessageDTO.setData(mainMessage);
                    Session pushSession = sessionMap.get(toFtsId);
                    if(pushSession != null && pushSession.isOpen()) {
                        try {
                            String pushJson = objectMapper.writeValueAsString(pushMessageDTO);
                            pushSession.getAsyncRemote().sendText(pushJson);
                        } catch (JsonProcessingException e) {
                            log.error("ObjectMapper write pushJson error,dto={}", pushMessageDTO, e);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                responseDTO.setSuccess(false);
                responseDTO.setMessage(WebSocketConstant.DATA_STRUCTURE_INVALID);
            }
        }
    }

    private void retrieveMessageList(WebSocketDTO requestDTO, WebSocketResponseDTO responseDTO, long ftsId) {
        responseDTO.setType(WebSocketDTO.WebSocketDTOType.RETRIEVE_MESSAGE_LIST_RESULT);
        Object data = requestDTO.getData();
        if(data == null) {
            responseDTO.setSuccess(false);
            responseDTO.setMessage(WebSocketConstant.INVALID_PARAM);
        } else {
            try {
                long activeUserFtsId = Long.parseLong(data.toString());
                List<WebSocketMainMessage> mainMessageList = userService.queryMessageListByTwoFtsIds(ftsId, activeUserFtsId);
                responseDTO.setSuccess(true);
                responseDTO.setData(mainMessageList);
            } catch (NumberFormatException e) {
                responseDTO.setSuccess(false);
                responseDTO.setMessage(WebSocketConstant.FTS_ID_NOT_A_NUMBER);
            }
        }
    }

    private void addFriend(WebSocketDTO requestDTO, WebSocketResponseDTO responseDTO, long ftsId) {
        responseDTO.setType(WebSocketDTO.WebSocketDTOType.ADD_FRIEND_RESULT);
        Object data = requestDTO.getData();
        long anotherFtsId;
        if(data == null) {
            responseDTO.setSuccess(false);
            responseDTO.setMessage(WebSocketConstant.INVALID_PARAM);
        } else {
            try {
                anotherFtsId = Long.parseLong(data.toString());
                Response<?> addFriendResponse = userService.addFriend(ftsId, anotherFtsId);
                responseDTO.setSuccess(addFriendResponse.isSuccess());
                if(!addFriendResponse.isSuccess()) {
                    responseDTO.setMessage(addFriendResponse.getMessage());
                } else {
                    Map<String,  Object> responseDataMap = new HashMap<>();
                    Map<String, Object> addFriendResponseDataMap = (Map<String, Object>) addFriendResponse.getData();
                    responseDataMap.put("nickname", addFriendResponseDataMap.get("anotherNickname"));
                    responseDataMap.put("userFtsId", addFriendResponseDataMap.get("anotherFtsId"));
                    responseDataMap.put("helloMessage", addFriendResponseDataMap.get("helloMessage"));
                    responseDTO.setData(responseDataMap);
                    WebSocketDTO pushMessageDTO  = new WebSocketDTO();
                    pushMessageDTO.setType(WebSocketDTO.WebSocketDTOType.MESSAGE);
                    WebSocketLeftMessage pushMessageData = new WebSocketLeftMessage();
                    pushMessageData.setText(WebSocketConstant.ADD_FRIEND_HELLO_MESSAGE);
                    pushMessageData.setType(Message.MessageType.text);
                    pushMessageData.setFromFtsId((long)addFriendResponseDataMap.get("ftsId"));
                    pushMessageData.setFromNickname((String) addFriendResponseDataMap.get("nickname"));
                    pushMessageData.setToFtsId(ftsId);
                    pushMessageData.setCreateTime(LocalDateTime.now().format(WebConfig.DATE_TIME_FORMATTER));
                    pushMessageDTO.setData(pushMessageData);
                    Session pushSession = sessionMap.get(anotherFtsId);
                    if(pushSession != null && pushSession.isOpen()) {
                        try {
                            String pushJson = objectMapper.writeValueAsString(pushMessageDTO);
                            pushSession.getAsyncRemote().sendText(pushJson);
                        } catch (JsonProcessingException e) {
                            log.error("ObjectMapper write pushJson error,dto={}", pushMessageDTO, e);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                responseDTO.setSuccess(false);
                responseDTO.setMessage(WebSocketConstant.FTS_ID_NOT_A_NUMBER);
            }
        }
    }

    @OnError
    public void onError(@PathParam("token")String token, Session session, Throwable ex) {
        log.error("onError", ex);
        Assert.isTrue(StringUtils.isNotBlank(token), "ws connection token blank");
        Long ftsId = userService.getFtsIdByToken(token, false);
        log.error("ws connection error token={} ftsId={} sessionId={}", token, ftsId, session.getId(), ex);
    }

    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{\"type\":\"ADD_FRIEND\",\"data\":10000}";
        WebSocketDTO webSocketDTO = objectMapper.readValue(json, new TypeReference<WebSocketDTO>() {});
        System.out.println(webSocketDTO);
    }

}
