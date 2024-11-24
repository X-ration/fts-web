package com.adam.ftsweb.service;

import com.adam.ftsweb.config.WebConfig;
import com.adam.ftsweb.constant.LoginPageConstant;
import com.adam.ftsweb.dto.RegisterForm;
import com.adam.ftsweb.mapper.UserMapper;
import com.adam.ftsweb.po.User;
import com.adam.ftsweb.po.UserExtend;
import com.adam.ftsweb.util.Response;
import com.adam.ftsweb.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.Objects;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public Response<Long> loginByFtsId(long ftsId, String password, boolean rememberMe) {
        Assert.isTrue(StringUtils.isNotBlank(password), "loginByFtsId password blank");
        User user = userMapper.queryUserByFtsId(ftsId);
        if(user != null) {
            String encryptedPassword = user.getPassword(), salt = user.getSalt();
            boolean checkPassword = StringUtil.checkPasswordMD5(password, encryptedPassword, salt);
            if(checkPassword) {
                return Response.success(ftsId);
            } else {
                return Response.fail(LoginPageConstant.FTS_ID_OR_PASSWORD_WRONG);
            }
        } else {
            return Response.fail(LoginPageConstant.USER_NOT_FOUND);
        }
    }

    public Response<Long> loginByEmail(String email, String password, boolean rememberMe) {
        Assert.isTrue(StringUtils.isNotBlank(email), "loginByEmail email blank");
        Assert.isTrue(StringUtils.isNotBlank(password), "loginByEmail password blank");
        Assert.isTrue(email.length() < 256 && StringUtil.isEmail(email), "loginByEmail email invalid");
        User user = userMapper.queryUserByEmail(email);
        if(user != null) {
            String encryptedPassword = user.getPassword(), salt = user.getSalt();
            boolean checkPassword = StringUtil.checkPasswordMD5(password, encryptedPassword, salt);
            if(checkPassword) {
                return Response.success(user.getFtsId());
            } else {
                return Response.fail(LoginPageConstant.EMAIL_OR_PASSWORD_WRONG);
            }
        } else {
            return Response.fail(LoginPageConstant.USER_NOT_FOUND);
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

}
