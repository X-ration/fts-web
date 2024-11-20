package com.adam.ftsweb.mapper;

import org.apache.ibatis.annotations.Select;

public interface TestMapper {

    @Select("SELECT max_fts_id FROM user_fts_id LIMIT 1")
    long queryMaxFtsId();

}
