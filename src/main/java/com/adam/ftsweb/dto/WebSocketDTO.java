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
        ADD_FRIEND_RESULT,
        /**
         * 初始化消息列表
         */
        INITIAL_MESSAGE_LIST,
        /**
         * 选中用户获取消息列表
         */
        RETRIEVE_MESSAGE_LIST,
        /**
         * 选中用户获取消息列表返回数据
         */
        RETRIEVE_MESSAGE_LIST_RESULT,
        /**
         * 发送文本消息
         */
        SEND_MESSAGE_TEXT,
        /**
         * 发送文本消息的返回数据
         */
        SEND_MESSAGE_TEXT_RESULT,
        ;
    }

}
