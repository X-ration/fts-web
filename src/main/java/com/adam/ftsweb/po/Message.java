package com.adam.ftsweb.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {

    private long id;
    private long fromFtsId;
    private long toFtsId;
    private String text;
    private MessageType messageType;
    private String fileUrl;
    private boolean isShow;
    private LocalDateTime createTime;

    public enum MessageType {
        text,file
    }
}
