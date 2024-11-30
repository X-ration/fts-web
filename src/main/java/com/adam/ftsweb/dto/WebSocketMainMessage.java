package com.adam.ftsweb.dto;

import com.adam.ftsweb.po.Message;
import lombok.Data;

/**
 * 当WebSocketDTO.type=RETRIEVE_MESSAGE_LIST时，data的结构类型
 * 只用于推送主栏消息
 */
@Data
public class WebSocketMainMessage {
    private String text;
    private Message.MessageType type;
    private long fromFtsId;
    private String fileUrl;
    /**
     * Jackson全局配置在这里不生效，需要手动转化格式
     */
    private String createTime;
}
