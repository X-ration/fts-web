package com.adam.ftsweb.common;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * UserService里的userTokenToFtsIdMap存储的value对象
 */
@Data
public class UserTokenMapItem {

    private long ftsId;
    private LocalDateTime createTime,updateTime;
    private long expireSeconds;

}
