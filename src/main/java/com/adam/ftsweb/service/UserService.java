package com.adam.ftsweb.service;

import com.adam.ftsweb.common.UserTokenMapItem;
import com.adam.ftsweb.config.WebConfig;
import com.adam.ftsweb.constant.LoginPageConstant;
import com.adam.ftsweb.constant.SystemConstant;
import com.adam.ftsweb.constant.WebSocketConstant;
import com.adam.ftsweb.dto.RegisterForm;
import com.adam.ftsweb.dto.WebSocketLeftMessage;
import com.adam.ftsweb.dto.WebSocketMainMessage;
import com.adam.ftsweb.mapper.FriendRelationshipMapper;
import com.adam.ftsweb.mapper.MessageMapper;
import com.adam.ftsweb.mapper.UserMapper;
import com.adam.ftsweb.po.FriendRelationship;
import com.adam.ftsweb.po.Message;
import com.adam.ftsweb.po.User;
import com.adam.ftsweb.po.UserExtend;
import com.adam.ftsweb.util.Response;
import com.adam.ftsweb.util.StringUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FriendRelationshipMapper friendRelationshipMapper;
    @Autowired
    private MessageMapper messageMapper;
    private final BiMap<String, UserTokenMapItem> userTokenToFtsIdMap = Maps.synchronizedBiMap(HashBiMap.create());

    public Response<Long> loginByFtsId(long ftsId, String password, boolean rememberMe, HttpSession session, HttpServletResponse response) {
        Assert.isTrue(StringUtils.isNotBlank(password), "loginByFtsId password blank");
        User user = userMapper.queryUserByFtsId(ftsId);
        if(user != null) {
            return login(user, password, rememberMe, session, response);
        } else {
            return Response.fail(LoginPageConstant.USER_NOT_FOUND);
        }
    }

    public Response<Long> loginByEmail(String email, String password, boolean rememberMe, HttpSession session, HttpServletResponse response) {
        Assert.isTrue(StringUtils.isNotBlank(email), "loginByEmail email blank");
        Assert.isTrue(StringUtils.isNotBlank(password), "loginByEmail password blank");
        Assert.isTrue(email.length() < 256 && StringUtil.isEmail(email), "loginByEmail email invalid");
        User user = userMapper.queryUserByEmail(email);
        if(user != null) {
            return login(user, password, rememberMe, session, response);
        } else {
            return Response.fail(LoginPageConstant.USER_NOT_FOUND);
        }
    }

    private Response<Long> login(User user, String password, boolean rememberMe, HttpSession session, HttpServletResponse response) {
        String encryptedPassword = user.getPassword(), salt = user.getSalt();
        boolean checkPassword = StringUtil.checkPasswordMD5(password, encryptedPassword, salt);
        if(checkPassword) {
            String token = StringUtil.generate32digitRandomUUID();
            UserTokenMapItem userTokenMapItem = new UserTokenMapItem();
            userTokenMapItem.setFtsId(user.getFtsId());
            LocalDateTime now = LocalDateTime.now();
            userTokenMapItem.setCreateTime(now);
            userTokenMapItem.setUpdateTime(now);
            userTokenMapItem.setExpireSeconds(SystemConstant.USER_TOKEN_TO_FTS_ID_MAP_DEFAULT_EXPIRES);
            userTokenToFtsIdMap.put(token, userTokenMapItem);
            session.setAttribute(SystemConstant.SESSION_LOGIN_FTS_TOKEN_KEY, token);
            if(rememberMe) {
                Cookie cookie = new Cookie(SystemConstant.COOKIE_LOGIN_FTS_TOKEN_KEY, token);
                cookie.setMaxAge(SystemConstant.COOKIE_LOGIN_FTS_TOKEN_MAX_AGE);
                cookie.setPath("/");
                response.addCookie(cookie);
            }
            return Response.success(user.getFtsId());
        } else {
            return Response.fail(LoginPageConstant.FTS_ID_OR_PASSWORD_WRONG);
        }
    }

    /**
     * 仅供测试
     * @param token
     * @return
     */
    public UserTokenMapItem getUserTokenMapItem(String token) {
        return userTokenToFtsIdMap.get(token);
    }

    /**
     * token换ftsId，自动清理过期项，自动延期
     * @param token
     * @param refresh 是否更新过期时间
     * @return
     */
    public Long getFtsIdByToken(String token, boolean refresh) {
        Assert.isTrue(StringUtils.isNotBlank(token), "getFtsIdByToken token blank");
        UserTokenMapItem userTokenMapItem = userTokenToFtsIdMap.get(token);
        if(userTokenMapItem == null) {
            return null;
        } else {
            LocalDateTime createTime = userTokenMapItem.getCreateTime(),
                    updateTime = userTokenMapItem.getUpdateTime(),
                    expireTime = createTime.plusSeconds(userTokenMapItem.getExpireSeconds()),
                    now = LocalDateTime.now();
            if(!expireTime.isAfter(now)) {
                log.debug("手动清理userTokenToFtsIdMap token={}", token);
                userTokenToFtsIdMap.remove(token);
                return null;
            } else {
                if(refresh) {
                    long addSeconds = ChronoUnit.SECONDS.between(updateTime, now);
                    userTokenMapItem.setExpireSeconds(userTokenMapItem.getExpireSeconds() + addSeconds);
                    userTokenMapItem.setUpdateTime(now);
                }
                return userTokenMapItem.getFtsId();
            }
        }
    }

    @Scheduled(cron = "0 0,30 *  * * ?")
    public void automaticCleanExpiredUserTokenMapItems() {
        log.info("[Scheduled]UserService automaticCleanExpiredUserTokenMapItems runs");
        for(Iterator<Map.Entry<String, UserTokenMapItem>> iterator = userTokenToFtsIdMap.entrySet().iterator(); iterator.hasNext();) {
//        for (Map.Entry<String, UserTokenMapItem> entry: userTokenToFtsIdMap.entrySet()) {
            Map.Entry<String, UserTokenMapItem> entry = iterator.next();
            String token = entry.getKey();
            UserTokenMapItem userTokenMapItem = entry.getValue();
            LocalDateTime createTime = userTokenMapItem.getCreateTime(),
                    expireTime = createTime.plusSeconds(userTokenMapItem.getExpireSeconds());
            if(!expireTime.isAfter(LocalDateTime.now())) {{
                iterator.remove();
                log.debug("自动清理userTokenToFtsIdMap token={}", token);
            }}
        }
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
        int count = userMapper.queryUserCountByFtsId(ftsId);
        if(count == 0) {
            return Response.fail(WebSocketConstant.ADD_FRIEND_USER_NOT_EXISTS);
        }
        count = userMapper.queryUserCountByFtsId(anotherFtsId);
        if(count == 0) {
            return Response.fail(WebSocketConstant.ADD_FRIEND_USER_NOT_EXISTS);
        }
        count = friendRelationshipMapper.queryFriendRelationshipCount(ftsId, anotherFtsId, FriendRelationship.FriendRelationshipAddType.web);
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
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("anotherNickname", userMapper.queryNicknameByFtsId(anotherFtsId));
        resultMap.put("anotherFtsId", anotherFtsId);
        resultMap.put("ftsId", ftsId);
        resultMap.put("nickname", userMapper.queryNicknameByFtsId(ftsId));
        resultMap.put("helloMessage", WebSocketConstant.ADD_FRIEND_HELLO_MESSAGE);
        return Response.success(resultMap);
    }

    /**
     * 用两个fts号码双向查询所有消息，按消息发送时间升序排序，过滤一条打招呼的消息
     * @param ftsId
     * @param anotherFtsId
     * @return
     */
    public List<WebSocketMainMessage> queryMessageListByTwoFtsIds(long ftsId, long anotherFtsId) {
        List<Message> messageList = messageMapper.queryMessageListByTwoFtsIds(ftsId, anotherFtsId),
                anotherMessageList = messageMapper.queryMessageListByTwoFtsIds(anotherFtsId, ftsId);
        if(!CollectionUtils.isEmpty(messageList)) {
            messageList.remove(0); //过滤第一条打招呼消息
            messageList.addAll(anotherMessageList);
        } else {
            messageList = anotherMessageList;
        }
        if(CollectionUtils.isEmpty(messageList)) {
            return new ArrayList<>();
        }
        messageList.sort(Comparator.comparing(Message::getCreateTime));
        List<WebSocketMainMessage> mainMessageList = messageList.stream().map(message -> {
            WebSocketMainMessage mainMessage = new WebSocketMainMessage();
            mainMessage.setText(message.getText());
            mainMessage.setToFtsId(message.getToFtsId());
            mainMessage.setType(message.getMessageType());
            mainMessage.setFromFtsId(message.getFromFtsId());
            mainMessage.setFileUrl(message.getFileUrl());
            mainMessage.setCreateTime(message.getCreateTime().format(WebConfig.DATE_TIME_FORMATTER));
            return mainMessage;
        }).collect(Collectors.toList());
        return mainMessageList;
    }

    /**
     * 查询某fts号码收到的所有消息，<strong>每个fts号码发来的消息只保留一条最新的</strong>
     * @param ftsId
     * @return
     */
    public List<WebSocketLeftMessage> queryMessageListByFtsId(long ftsId) {
        List<Message> messageList = messageMapper.queryMessageListByFtsId(ftsId);
        if(CollectionUtils.isEmpty(messageList)) {
            return new ArrayList<>();
        }
        Set<Long> userFtsIds = new HashSet<>();
        messageList.forEach(message -> userFtsIds.add(message.getFromFtsId()));
        List<Map<String,Object>> nicknameResultList = userMapper.queryNicknamesByFtsIds(userFtsIds);
        Map<Long, String> nicknameMap = new HashMap<>();
        nicknameResultList.forEach(stringObjectMap -> nicknameMap.put((long)stringObjectMap.get("fts_id"),
                (String)stringObjectMap.get("nickname")));
        List<WebSocketLeftMessage> webSocketLeftMessageList = new LinkedList<>();
        messageList.forEach(message -> {
            WebSocketLeftMessage webSocketLeftMessage = new WebSocketLeftMessage();
            webSocketLeftMessage.setType(message.getMessageType());
            webSocketLeftMessage.setToFtsId(message.getToFtsId());
            webSocketLeftMessage.setText(message.getText());
            webSocketLeftMessage.setFromFtsId(message.getFromFtsId());
            webSocketLeftMessage.setFromNickname(nicknameMap.get(message.getFromFtsId()));
            webSocketLeftMessage.setCreateTime(message.getCreateTime().format(WebConfig.DATE_TIME_FORMATTER));
            webSocketLeftMessageList.add(webSocketLeftMessage);
        });
        Set<Long> fromUserFtsIds = new HashSet<>();
        for(Iterator<WebSocketLeftMessage> iterator = webSocketLeftMessageList.iterator(); iterator.hasNext(); ) {
            WebSocketLeftMessage webSocketLeftMessage = iterator.next();
            if(fromUserFtsIds.contains(webSocketLeftMessage.getFromFtsId())) {
                iterator.remove();
            } else {
                fromUserFtsIds.add(webSocketLeftMessage.getFromFtsId());
            }
        }
        return webSocketLeftMessageList;
    }

    @Transactional
    public Response<Long> registerUser(RegisterForm registerForm) {
        Assert.notNull(registerForm, "registerForm null");
        Assert.notNull(registerForm.getEmail(), "registerForm.email null");
        Assert.notNull(registerForm.getNickname(), "registerForm.nickname null");
        Assert.notNull(registerForm.getPassword(), "registerForm.password null");
        Assert.isTrue(StringUtils.equals(registerForm.getPassword(), registerForm.getVerifyPassword()), "registerForm password not equal");
        try {
            int count = userMapper.queryUserCountByEmail(registerForm.getEmail());
            if (count > 0) {
                return Response.fail("注册失败：电子邮件地址已被使用");
            }
            long ftsId = userMapper.queryMaxFtsId() + 1;
            userMapper.incrementUserFtsId();
            log.info("registerUser email[{}] ftsId={}", registerForm.getEmail(), ftsId);

            String salt = StringUtil.generatePasswordSalt();
            String encryptedPassword = StringUtil.encryptPasswordMD5(registerForm.getPassword(), salt);
            User user = new User();
            user.setFtsId(ftsId);
            user.setEmail(registerForm.getEmail());
            user.setNickname(registerForm.getNickname());
            user.setPassword(encryptedPassword);
            user.setSalt(salt);
            user.setEnabled(true);
            userMapper.insertUser(user);

            UserExtend userExtend = new UserExtend();
            userExtend.setUserId(user.getId());
            if(StringUtils.isNotBlank(registerForm.getBirthDate())) {
                LocalDate birthDate = LocalDate.parse(registerForm.getBirthDate(), WebConfig.DATE_FORMATTER);
                userExtend.setBirthDate(birthDate);
            }
            if(StringUtils.isNotBlank(registerForm.getHobby())) {
                userExtend.setHobby(registerForm.getHobby());
            }
            if(StringUtils.isNotBlank(registerForm.getAutograph())) {
                userExtend.setAutograph(registerForm.getAutograph());
            }
            userMapper.insertUserExtend(userExtend);
            return Response.success(ftsId);
        } catch (Exception e) {
            log.error("registerUser exception", e);
            return Response.fail("注册失败，请稍候再试");
        }
    }

}
