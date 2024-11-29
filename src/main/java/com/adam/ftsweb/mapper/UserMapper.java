package com.adam.ftsweb.mapper;

import com.adam.ftsweb.po.User;
import com.adam.ftsweb.po.UserExtend;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserMapper {

    @Select("SELECT max_fts_id FROM user_fts_id LIMIT 1")
    long queryMaxFtsId();

    @Update("UPDATE user_fts_id SET max_fts_id=max_fts_id+1")
    int incrementUserFtsId();

    @Select("SELECT COUNT(*) FROM user WHERE email=#{email} AND is_enabled=true")
    int queryUserCountByEmail(String email);

    @Insert({"INSERT INTO user (fts_id,email,nickname,password,salt,is_enabled)",
            "VALUES (#{ftsId},#{email},#{nickname},#{password},#{salt},#{isEnabled})"
    })
    @Options(useGeneratedKeys=true, keyProperty="id")
    int insertUser(User user);

    @Insert({"INSERT INTO user_extend (user_id,birth_date,hobby,autograph)",
            "VALUES (#{userId},#{birthDate},#{hobby},#{autograph})"
    })
    @Options(useGeneratedKeys=true, keyProperty="id")
    int insertUserExtend(UserExtend userExtend);

    @Select("SELECT COUNT(*) FROM user WHERE fts_id=#{ftsId} AND is_enabled=true")
    int queryUserCountByFtsId(long ftsId);

    @Select("SELECT fts_id FROM user WHERE email=#{email} AND is_enabled=true LIMIT 1")
    Long queryFtsIdByEmail(String email);

    @Select({"SELECT id,fts_id,email,nickname,password,salt,is_enabled,create_time,update_time",
            "FROM user",
            "WHERE fts_id=#{ftsId} AND is_enabled=true"
    })
    @Results({
            @Result(id = true, column = "id", property = "id"),
            @Result(column = "fts_id", property = "ftsId"),
            @Result(column = "email", property = "email"),
            @Result(column = "nickname", property = "nickname"),
            @Result(column = "password", property = "password"),
            @Result(column = "salt", property = "salt"),
            @Result(column = "is_enabled", property = "isEnabled"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(property = "userExtend", column = "id", one = @One(select =
                    "com.adam.ftsweb.mapper.UserMapper.queryUserExtendByUserId"))
    })
    User queryUserByFtsId(long ftsId);

    @Select({"SELECT id,fts_id,email,nickname,password,salt,is_enabled,create_time,update_time",
            "FROM user",
            "WHERE email=#{email} AND is_enabled=true"
    })
    @Results({
            @Result(id = true, column = "id", property = "id"),
            @Result(column = "fts_id", property = "ftsId"),
            @Result(column = "email", property = "email"),
            @Result(column = "nickname", property = "nickname"),
            @Result(column = "password", property = "password"),
            @Result(column = "salt", property = "salt"),
            @Result(column = "is_enabled", property = "isEnabled"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime"),
            @Result(property = "userExtend", column = "id", one = @One(select =
                    "com.adam.ftsweb.mapper.UserMapper.queryUserExtendByUserId"))
    })
    User queryUserByEmail(String email);

    @Select({"SELECT id,user_id,birth_date,hobby,autograph,create_time,update_time",
            "FROM user_extend",
            "WHERE user_id=#{userId}"
    })
    @Results({
            @Result(id = true, column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "birth_date", property = "birthDate"),
            @Result(column = "hobby", property = "hobby"),
            @Result(column = "autograph", property = "autograph"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")
    })
    UserExtend queryUserExtendByUserId(long userId);

    @Select("SELECT nickname FROM user WHERE fts_id=#{userFtsId}")
    String queryNicknameByFtsId(long userFtsId);

    @Select({
            "<script>",
            "SELECT fts_id,nickname FROM user WHERE fts_id IN ",
            "<foreach collection=\"userFtsIds\" item=\"item\" separator=\",\" open=\"(\" close=\")\">",
            "#{item}",
            "</foreach>",
            "</script>"
    })
    List<Map<String, Object>> queryNicknamesByFtsIds(@Param("userFtsIds") Set<Long> userFtsIds);

}
