package com.adam.ftsweb.service;

import com.adam.ftsweb.constant.WebSocketConstant;
import com.adam.ftsweb.mapper.FriendRelationshipMapper;
import com.adam.ftsweb.mapper.MessageMapper;
import com.adam.ftsweb.po.FriendRelationship;
import com.adam.ftsweb.po.Message;
import com.adam.ftsweb.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class FriendRelationshipService {

    @Autowired
    private FriendRelationshipMapper friendRelationshipMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UserService userService;

    public Map<Long, LocalDateTime> queryFriendFtsIdToCreateTimeMap(long ftsId) {
        List<Map<String,Object>> friendFtsIdListWithCreateTime = friendRelationshipMapper.queryFriendFtsIdsWithCreateTime(ftsId, FriendRelationship.FriendRelationshipAddType.web);
        Map<Long,LocalDateTime> friendFtsIdToCreateTimeMap = new HashMap<>();
        friendFtsIdListWithCreateTime.forEach(stringObjectMap -> friendFtsIdToCreateTimeMap.put((long)stringObjectMap.get("user_fts_id"), (LocalDateTime) stringObjectMap.get("create_time")));
        return friendFtsIdToCreateTimeMap;
    }

    /**
     * 为了后续查询的方便，双向添加好友关系
     * @param ftsId
     * @param anotherFtsId
     * @return
     */
    @Transactional
    public Response<?> addFriend(Long ftsId, Long anotherFtsId) {
        Assert.notNull(ftsId, "addFriend ftsId null");
        Assert.notNull(anotherFtsId, "addFriend anotherFtsId null");
        if(ftsId.equals(anotherFtsId)) {
            return Response.fail(WebSocketConstant.ADD_FRIEND_WITH_SELF);
        }
        if(!userService.userExists(ftsId) || !userService.userExists(anotherFtsId)) {
            return Response.fail(WebSocketConstant.USER_NOT_EXISTS);
        }
        int count = friendRelationshipMapper.queryFriendRelationshipCount(ftsId, anotherFtsId, FriendRelationship.FriendRelationshipAddType.web);
        if(count > 0) {
            return Response.fail(WebSocketConstant.ADD_FRIEND_ALREADY_ADDED);
        }
        FriendRelationship friendRelationship = new FriendRelationship();
        friendRelationship.setUserFtsId(ftsId);
        friendRelationship.setAnotherUserFtsId(anotherFtsId);
        friendRelationship.setAddType(FriendRelationship.FriendRelationshipAddType.web);
        friendRelationshipMapper.insertFriendRelationship(friendRelationship);
        //双向添加好友关系
        friendRelationship.setUserFtsId(anotherFtsId);
        friendRelationship.setAnotherUserFtsId(ftsId);
        friendRelationshipMapper.insertFriendRelationship(friendRelationship);

        //双向发送打招呼消息
        Message helloMessage = new Message();
        helloMessage.setFromFtsId(ftsId);
        helloMessage.setToFtsId(anotherFtsId);
        helloMessage.setText(WebSocketConstant.ADD_FRIEND_HELLO_MESSAGE);
        helloMessage.setMessageType(Message.MessageType.text);
        messageMapper.insertMessage(helloMessage);
        helloMessage.setFromFtsId(anotherFtsId);
        helloMessage.setToFtsId(ftsId);
        messageMapper.insertMessage(helloMessage);
        Set<Long> ftsIdSet = new HashSet<>();
        ftsIdSet.add(ftsId);
        ftsIdSet.add(anotherFtsId);
        Map<String,Object> resultMap = new HashMap<>();
        Map<Long,String> nicknameMap = userService.queryFtsIdToNicknameMap(ftsIdSet);
        resultMap.put("anotherNickname", nicknameMap.get(anotherFtsId));
        resultMap.put("anotherFtsId", anotherFtsId);
        resultMap.put("ftsId", ftsId);
        resultMap.put("nickname", nicknameMap.get(ftsId));
        resultMap.put("helloMessage", WebSocketConstant.ADD_FRIEND_HELLO_MESSAGE);
        return Response.success(resultMap);
    }

}
