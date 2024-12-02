package com.adam.ftsweb.constant;

public class SystemConstant {

    public static final String SESSION_LOGIN_FTS_TOKEN_KEY = "loginFtsToken";
    public static final String COOKIE_LOGIN_FTS_TOKEN_KEY = "loginFtsToken";
    /**
     * Cookie最长保存时间为7天-记住登录状态的最长时间
     */
    public static final int COOKIE_LOGIN_FTS_TOKEN_MAX_AGE = 60 * 60 * 24 * 7;

    /**
     * 为UserTokenToFtsIdMap中的对象指定默认过期时间为7天
     * todo 建立连接时考虑更新过期时间
     */
    public static final long USER_TOKEN_TO_FTS_ID_MAP_DEFAULT_EXPIRES = 60 * 60 * 24 * 7;

    /**
     * 文件过期时间为7天
     */
    public static final long FILE_EXPIRE_TIME_MILLS = 1000 * 60 * 60 * 24 * 7;
}
