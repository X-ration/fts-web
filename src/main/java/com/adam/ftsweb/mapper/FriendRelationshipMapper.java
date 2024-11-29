package com.adam.ftsweb.mapper;

import com.adam.ftsweb.po.FriendRelationship;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FriendRelationshipMapper {

    @Insert({
            "INSERT INTO friend_relationship (user_fts_id,another_user_fts_id,add_type)",
            "VALUES (#{userFtsId},#{anotherUserFtsId},#{addType})"
    })
    int insertFriendRelationship(FriendRelationship friendRelationship);

    @Select("SELECT COUNT(*) FROM friend_relationship WHERE user_fts_id=#{userFtsId} AND another_user_fts_id=#{anotherUserFtsId} AND add_type=#{addType}")
    int queryFriendRelationshipCount(@Param("userFtsId") long userFtsId, @Param("anotherUserFtsId") long anotherUserFtsId,
                                     @Param("addType") FriendRelationship.FriendRelationshipAddType addType);

}