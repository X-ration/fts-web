package com.adam.ftsweb.po;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对应数据库user_extend表
 */
@Data
public class UserExtend {
    private long id;
    private long userId;
    private LocalDate birthDate;
    private String hobby;
    private String autograph;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}