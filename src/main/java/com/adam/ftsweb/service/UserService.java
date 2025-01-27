package com.adam.ftsweb.service;

import com.adam.ftsweb.common.UserTokenMapItem;
import com.adam.ftsweb.config.WebConfig;
import com.adam.ftsweb.constant.LoginPageConstant;
import com.adam.ftsweb.constant.SystemConstant;
import com.adam.ftsweb.constant.WebSocketConstant;
import com.adam.ftsweb.dto.*;
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
    private final Map<Long, List<String>> ftsIdToTokenMap = Collections.synchronizedMap(new HashMap<>());

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
            List<String> tokenList = ftsIdToTokenMap.computeIfAbsent(user.getFtsId(), k -> new LinkedList<>());
            tokenList.add(token);
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

    public void expireTokens(long ftsId) {
        List<String> tokenList = ftsIdToTokenMap.get(ftsId);
        if(!CollectionUtils.isEmpty(tokenList)) {
            log.info("Expiring token ftsId={} tokenCount={}", ftsId, tokenList.size());
            for(String token: tokenList) {
                userTokenToFtsIdMap.remove(token);
            }
            ftsIdToTokenMap.remove(ftsId);
        }
    }

    @Deprecated
    public void expireToken(String token) {
        userTokenToFtsIdMap.remove(token);
    }

    public Long searchUser(Object search) {
        Assert.notNull(search, "searchUser search null");
        String searchString = search instanceof String ? (String) search : search.toString();
        try {
            long ftsId = Long.parseLong(searchString);
            int count = userMapper.queryUserCountByFtsId(ftsId);
            if(count > 0) {
                return ftsId;
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return userMapper.queryFtsIdByEmail(searchString);
        }
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
                List<String> tokenList = ftsIdToTokenMap.get(userTokenMapItem.getFtsId());
                if(!CollectionUtils.isEmpty(tokenList)) {
                    tokenList.remove(token);
                }
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
                List<String> tokenList = ftsIdToTokenMap.get(userTokenMapItem.getFtsId());
                if(!CollectionUtils.isEmpty(tokenList)) {
                    tokenList.remove(token);
                }
                log.debug("自动清理userTokenToFtsIdMap token={}", token);
            }}
        }
        log.info("[Scheduled]UserService automaticCleanExpiredUserTokenMapItems ends");
    }

    public boolean userExists(long ftsId) {
        int count = userMapper.queryUserCountByFtsId(ftsId);
        return count > 0;
    }

    public String queryNicknameByFtsId(long ftsId) {
        return userMapper.queryNicknameByFtsId(ftsId);
    }

    public Map<Long,String> queryFtsIdToNicknameMap(Set<Long> ftsIds) {
        List<Map<String,Object>> nicknameResultList = userMapper.queryNicknamesByFtsIds(ftsIds);
        Map<Long, String> nicknameMap = new HashMap<>();
        nicknameResultList.forEach(stringObjectMap -> nicknameMap.put((long)stringObjectMap.get("fts_id"),
                (String)stringObjectMap.get("nickname")));
        return nicknameMap;
    }

    public Response<ProfileDTO> queryProfile(long ftsId) {
        if(!userExists(ftsId)) {
            return Response.fail(WebSocketConstant.USER_NOT_EXISTS);
        }
        User user = userMapper.queryUserByFtsId(ftsId);
        if(user == null) {
            return Response.fail(WebSocketConstant.USER_NOT_ENABLED);
        }
        ProfileDTO profileDTO = new ProfileDTO();
        profileDTO.setFtsId(user.getFtsId());
        profileDTO.setNickname(user.getNickname());
        profileDTO.setEmail(user.getEmail());
        //扩展信息可能为null
        if(user.getUserExtend().getBirthDate() != null) {
            profileDTO.setBirthDate(user.getUserExtend().getBirthDate().format(WebConfig.DATE_FORMATTER));
        }
        profileDTO.setHobby(user.getUserExtend().getHobby());
        profileDTO.setAutograph(user.getUserExtend().getAutograph());
        return Response.success(profileDTO);
    }

    public boolean checkModifyEmailHasProblem(long ftsId, String email) {
        Assert.isTrue(StringUtils.isNotBlank(email), "checkModifyEmailSame email blank");
        User user1 = userMapper.queryUserByFtsId(ftsId);
        if(user1 == null) {
            return false;
        }
        if(!StringUtils.equals(user1.getEmail(), email)) {
            int count = userMapper.queryUserCountByEmail(email);
            return count > 0;
        }
        return false;
    }

    @Transactional
    public Response<RegisterFormErrorMsg> updateUser(long ftsId, RegisterForm registerForm) {
        Assert.notNull(registerForm, "updateUser registerForm null");
        try {
            RegisterFormErrorMsg registerFormErrorMsg = new RegisterFormErrorMsg();
            if (!userExists(ftsId)) {
                return Response.fail(WebSocketConstant.USER_NOT_EXISTS);
            }
            User user1 = userMapper.queryUserByFtsId(ftsId);
            if(user1 == null) {
                return Response.fail(WebSocketConstant.USER_NOT_ENABLED);
            }
            String salt = StringUtil.generatePasswordSalt();
            String encryptedPassword = StringUtil.encryptPasswordMD5(registerForm.getPassword(), salt);
            User user = new User();
            user.setFtsId(ftsId);
            user.setNickname(registerForm.getNickname());
            user.setEmail(registerForm.getEmail());
            user.setPassword(encryptedPassword);
            user.setSalt(salt);
            userMapper.updateUserByFtsId(user);

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
            if(userExtend.getBirthDate() != null || userExtend.getHobby() != null || userExtend.getAutograph() != null) {
                userMapper.updateUserExtendByUserId(userExtend);
            }
            return Response.success();
        } catch (Exception e) {
            log.error("updateUser exception", e);
            return Response.fail("修改失败，请稍候再试");
        }
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

    public boolean userExistsByEmail(String email) {
        Assert.isTrue(StringUtils.isNotBlank(email), "userExistsByEmail email blank");
        return userMapper.queryUserCountByEmail(email) > 0;
    }

}
