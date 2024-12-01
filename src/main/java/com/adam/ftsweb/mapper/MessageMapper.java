package com.adam.ftsweb.mapper;

import com.adam.ftsweb.po.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface MessageMapper {

    @Insert({
            "INSERT INTO message (from_fts_id,to_fts_id,text,message_type,file_url)",
            "VALUES (#{fromFtsId},#{toFtsId},#{text},#{messageType},#{fileUrl})"
    })
    @Options(useGeneratedKeys=true, keyProperty="id")
    int insertMessage(Message message);

    @Select({
            "(SELECT id,from_fts_id,to_fts_id,text,message_type,file_url,is_show,create_time",
            "FROM message WHERE from_fts_id=#{ftsId} AND to_fts_id=#{anotherFtsId} and is_show=true",
            "ORDER BY id DESC LIMIT 1)",
            "UNION ALL",
            "(SELECT id,from_fts_id,to_fts_id,text,message_type,file_url,is_show,create_time",
            "FROM message WHERE from_fts_id=#{anotherFtsId} AND to_fts_id=#{ftsId} and is_show=true",
            "ORDER BY id DESC LIMIT 1)",
    })
    @Results({
            @Result(id = true, column = "id", property = "id"),
            @Result(column = "from_fts_id", property = "fromFtsId"),
            @Result(column = "to_fts_id", property = "toFtsId"),
            @Result(column = "text", property = "text"),
            @Result(column = "message_type", property = "messageType"),
            @Result(column = "file_url", property = "fileUrl"),
            @Result(column = "is_show", property = "isShow"),
            @Result(column = "create_time", property = "createTime")
    })
    List<Message> queryMessageListBothOneByTwoFtsIds(@Param("ftsId") long ftsId, @Param("anotherFtsId")long anotherFtsId);

    @Select({
            "SELECT id,from_fts_id,to_fts_id,text,message_type,file_url,is_show,create_time",
            "FROM message WHERE from_fts_id=#{ftsId} AND to_fts_id=#{anotherFtsId} and is_show=true",
            "ORDER BY id DESC"
    })
    @Results({
            @Result(id = true, column = "id", property = "id"),
            @Result(column = "from_fts_id", property = "fromFtsId"),
            @Result(column = "to_fts_id", property = "toFtsId"),
            @Result(column = "text", property = "text"),
            @Result(column = "message_type", property = "messageType"),
            @Result(column = "file_url", property = "fileUrl"),
            @Result(column = "is_show", property = "isShow"),
            @Result(column = "create_time", property = "createTime")
    })
    List<Message> queryMessageListByTwoFtsIds(@Param("ftsId")long ftsId, @Param("anotherFtsId")long anotherFtsId);

}
