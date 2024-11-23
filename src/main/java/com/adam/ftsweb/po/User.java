package com.adam.ftsweb.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应数据库user表
 */
@Data
public class User {
    private long id;
    private long ftsId;
    private String email;
    private String nickname;
    private String password;
    private String salt;
    private boolean isEnabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private UserExtend userExtend;
}