package com.adam.ftsweb.mapper;

import com.adam.ftsweb.po.Message;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface MessageMapper {

    @Insert({
            "INSERT INTO message (from_fts_id,to_fts_id,text,message_type,file_url)",
            "VALUES (#{fromFtsId},#{toFtsId},#{text},#{messageType},#{fileUrl})"
    })
    @Options(useGeneratedKeys=true, keyProperty="id")
    int insertMessage(Message message);

}
