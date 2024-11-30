package com.adam.ftsweb.dto;

import com.adam.ftsweb.po.Message;
import lombok.Data;

/**
 * 当WebSocketDTO.type=MESSAGE时，data的结构类型
 * 只用于推送左边栏消息
 */
@Data
public class WebSocketLeftMessage {
    private String text;
    private Message.MessageType type;
    private long fromFtsId;
    private long toFtsId;
    private String fromNickname;
    /**
     * Jackson全局配置在这里不生效，需要手动转化格式
     */
    private String createTime;
}
