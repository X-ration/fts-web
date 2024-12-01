package com.adam.ftsweb.mapper;

import com.adam.ftsweb.po.FriendRelationship;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface FriendRelationshipMapper {

    @Insert({
            "INSERT INTO friend_relationship (user_fts_id,another_user_fts_id,add_type)",
            "VALUES (#{userFtsId},#{anotherUserFtsId},#{addType})"
    })
    int insertFriendRelationship(FriendRelationship friendRelationship);

    @Select("SELECT COUNT(*) FROM friend_relationship WHERE user_fts_id=#{userFtsId} AND another_user_fts_id=#{anotherUserFtsId} AND add_type=#{addType}")
    int queryFriendRelationshipCount(@Param("userFtsId") long userFtsId, @Param("anotherUserFtsId") long anotherUserFtsId,
                                     @Param("addType") FriendRelationship.FriendRelationshipAddType addType);

    @Select("SELECT user_fts_id,create_time FROM friend_relationship WHERE another_user_fts_id=#{ftsId} AND add_type=#{addType}")
    List<Map<String,Object>> queryFriendFtsIdsWithCreateTime(@Param("ftsId")long ftsId, @Param("addType")FriendRelationship.FriendRelationshipAddType addType);

}