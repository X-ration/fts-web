package com.adam.ftsweb.po;

import lombok.Data;

@Data
public class FriendRelationship {

    private long userFtsId;
    private long anotherUserFtsId;
    private FriendRelationshipAddType addType;

    public enum FriendRelationshipAddType {
        web
    }
}
