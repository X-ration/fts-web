package com.adam.ftsweb.mapper;

import com.adam.ftsweb.po.User;
import com.adam.ftsweb.po.UserExtend;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

}
