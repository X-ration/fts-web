package com.adam.ftsweb.dto;

import com.adam.ftsweb.po.Message;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当WebSocketDTO.type=MESSAGE时，data的结构类型
 */
@Data
public class WebSocketMessage {
    private String text;
    private Message.MessageType type;
    private long fromFtsId;
    private String fromNickname;
    /**
     * Jackson全局配置在这里不生效，需要手动转化格式
     */
    private String createTime;
}
