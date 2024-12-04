package com.adam.ftsweb.service;

import com.adam.ftsweb.config.WebConfig;
import com.adam.ftsweb.constant.WebSocketConstant;
import com.adam.ftsweb.dto.WebSocketLeftMessage;
import com.adam.ftsweb.dto.WebSocketMainMessage;
import com.adam.ftsweb.mapper.MessageMapper;
import com.adam.ftsweb.po.Message;
import com.adam.ftsweb.util.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private FriendRelationshipService friendRelationshipService;

    /**
     * 将两个用户之间的所有消息is_show字段标记为false
     * @param ftsId
     * @param anotherFtsId
     * @return
     */
    public Response<Integer> clearAllMessages(long ftsId, long anotherFtsId) {
        if(!userService.userExists(ftsId) || !userService.userExists(anotherFtsId)) {
            return Response.fail(WebSocketConstant.USER_NOT_EXISTS);
        }
        int count = messageMapper.updateIsShowToFalseByTwoFtsIds(ftsId, anotherFtsId);
        return Response.success(count);
    }

    public List<WebSocketLeftMessage> queryMessageListByTwoFtsId(long ftsId, long friendFtsId) {
        Map<Long, LocalDateTime> friendFtsIdToCreateTimeMap = friendRelationshipService.queryFriendFtsIdToCreateTimeMap(ftsId);
        if(CollectionUtils.isEmpty(friendFtsIdToCreateTimeMap)) {
            return new ArrayList<>();
        } else if(!friendFtsIdToCreateTimeMap.containsKey(friendFtsId)) {
            return new ArrayList<>();
        }
        List<Message> oneMessageList = messageMapper.queryMessageListBothOneByTwoFtsIds(ftsId, friendFtsId);
        Message message;
        if(CollectionUtils.isEmpty(oneMessageList)) {
            message = new Message();
            message.setMessageType(Message.MessageType.text);
            message.setText("");
            message.setCreateTime(friendFtsIdToCreateTimeMap.get(friendFtsId));
            message.setFromFtsId(friendFtsId);
        } else if(oneMessageList.size() == 1) {
            message = oneMessageList.get(0);
        } else {
            Message message1 = oneMessageList.get(0), message2 = oneMessageList.get(1);
            int compare = message1.getCreateTime().compareTo(message2.getCreateTime());
            if(compare < 0) {
                message = message2;
            } else {
                message = message1;
            }
        }
        WebSocketLeftMessage leftMessage = new WebSocketLeftMessage();
        leftMessage.setType(message.getMessageType());
        leftMessage.setText(message.getText());
        leftMessage.setFromFtsId(message.getFromFtsId());
        leftMessage.setToFtsId(message.getToFtsId());
        leftMessage.setFromNickname(userService.queryNicknameByFtsId(message.getFromFtsId()));
        leftMessage.setCreateTime(message.getCreateTime().format(WebConfig.DATE_TIME_FORMATTER));
        List<WebSocketLeftMessage> leftMessageList = new ArrayList<>(1);
        leftMessageList.add(leftMessage);
        return leftMessageList;
    }

    /**
     * 查询某fts号码收到的所有消息，<strong>每个fts号码发来的消息只保留一条最新的</strong>
     * @param ftsId
     * @return
     */
    public List<WebSocketLeftMessage> queryMessageListByFtsId(long ftsId) {
        Map<Long, LocalDateTime> friendFtsIdToCreateTimeMap = friendRelationshipService.queryFriendFtsIdToCreateTimeMap(ftsId);
        if(CollectionUtils.isEmpty(friendFtsIdToCreateTimeMap)) {
            return new ArrayList<>();
        }
        Map<Long,String> nicknameMap = userService.queryFtsIdToNicknameMap(friendFtsIdToCreateTimeMap.keySet());
        List<Message> messageList = new LinkedList<>();
        for(Long friendFtsId: friendFtsIdToCreateTimeMap.keySet()) {
            List<Message> oneMessageList = messageMapper.queryMessageListBothOneByTwoFtsIds(ftsId, friendFtsId);
            Message message;
            if(CollectionUtils.isEmpty(oneMessageList)) {
                message = new Message();
                message.setMessageType(Message.MessageType.text);
                message.setText("");
                message.setCreateTime(friendFtsIdToCreateTimeMap.get(friendFtsId));
                message.setFromFtsId(friendFtsId);
                messageList.add(message);
            } else if(oneMessageList.size() == 1) {
                messageList.add(oneMessageList.get(0));
            } else {
                Message message1 = oneMessageList.get(0), message2 = oneMessageList.get(1);
                int compare = message1.getCreateTime().compareTo(message2.getCreateTime());
                if(compare < 0) {
                    messageList.add(message2);
                } else {
                    messageList.add(message1);
                }
            }
            messageList.get(messageList.size() - 1).setFromFtsId(friendFtsId);
            messageList.get(messageList.size() - 1).setToFtsId(0);
        }
        messageList.sort((o1, o2) -> -1 * o1.getCreateTime().compareTo(o2.getCreateTime()));
        List<WebSocketLeftMessage> resultList = messageList.stream().map(message -> {
            WebSocketLeftMessage leftMessage = new WebSocketLeftMessage();
            leftMessage.setType(message.getMessageType());
            leftMessage.setText(message.getText());
            leftMessage.setFromFtsId(message.getFromFtsId());
            leftMessage.setToFtsId(message.getToFtsId());
            leftMessage.setFromNickname(nicknameMap.get(message.getFromFtsId()));
            leftMessage.setCreateTime(message.getCreateTime().format(WebConfig.DATE_TIME_FORMATTER));
            return leftMessage;
        }).collect(Collectors.toList());
        return resultList;
    }

    public Response<?> sendMessage(long fromFtsId, long toFtsId, String text, Message.MessageType messageType, String fileUrl) {
        Assert.isTrue(StringUtils.isNotBlank(text), "sendMessage text blank");
        Assert.notNull(messageType, "sendMessage messageType null");
        if(messageType == Message.MessageType.file) {
            Assert.notNull(fileUrl, "sendMessage fileUrl null");
        }
        if(!userService.userExists(fromFtsId) || !userService.userExists(toFtsId)) {
            return Response.fail(WebSocketConstant.USER_NOT_EXISTS);
        }
        Message message = new Message();
        message.setMessageType(messageType);
        message.setFromFtsId(fromFtsId);
        message.setToFtsId(toFtsId);
        message.setText(text);
        if(messageType == Message.MessageType.file) {
            message.setFileUrl(fileUrl);
        }
        messageMapper.insertMessage(message);
        String nickname = userService.queryNicknameByFtsId(fromFtsId);
        return Response.success(nickname);
    }

    /**
     * 用两个fts号码双向查询所有消息，按消息发送时间升序排序
     * @param ftsId
     * @param anotherFtsId
     * @return
     */
    public List<WebSocketMainMessage> queryMessageListByTwoFtsIds(long ftsId, long anotherFtsId) {
        List<Message> messageList = messageMapper.queryMessageListByTwoFtsIds(ftsId, anotherFtsId);
        List<Message> anotherMessageList;
        if(ftsId != anotherFtsId) {
            anotherMessageList = messageMapper.queryMessageListByTwoFtsIds(anotherFtsId, ftsId);
        } else {
            anotherMessageList = new ArrayList<>();
        }
        if(!CollectionUtils.isEmpty(messageList)) {
            messageList.addAll(anotherMessageList);
        } else {
            messageList = anotherMessageList;
        }
        if(CollectionUtils.isEmpty(messageList)) {
            return new ArrayList<>();
        }
        Set<Long> ftsIds = new HashSet<>();
        messageList.forEach(message -> ftsIds.add(message.getFromFtsId()));
        Map<Long,String> nicknameMap = userService.queryFtsIdToNicknameMap(ftsIds);
        messageList.sort(Comparator.comparing(Message::getCreateTime));
        List<WebSocketMainMessage> mainMessageList = messageList.stream().map(message -> {
            WebSocketMainMessage mainMessage = new WebSocketMainMessage();
            mainMessage.setText(message.getText());
            mainMessage.setToFtsId(message.getToFtsId());
            mainMessage.setType(message.getMessageType());
            mainMessage.setFromFtsId(message.getFromFtsId());
            mainMessage.setFileUrl(message.getFileUrl());
            mainMessage.setCreateTime(message.getCreateTime().format(WebConfig.DATE_TIME_FORMATTER));
            mainMessage.setFromNickname(nicknameMap.get(message.getFromFtsId()));
            return mainMessage;
        }).collect(Collectors.toList());
        return mainMessageList;
    }



}
