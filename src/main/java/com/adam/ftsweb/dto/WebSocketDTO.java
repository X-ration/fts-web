package com.adam.ftsweb.dto;

import lombok.Data;

@Data
public class WebSocketDTO {

    private WebSocketDTOType type;
    private String message;
    private Object data;

    public enum WebSocketDTOType {
        /**
         * 普通消息
         */
        MESSAGE,
        /**
         * 无法解析的消息体
         */
        NOT_RESOLVABLE,
        /**
         * 添加好友的请求数据包类型
         */
        ADD_FRIEND,
        /**
         * 添加好友的响应数据包类型
         */
        ADD_FRIEND_RESULT
        ;
    }

}
