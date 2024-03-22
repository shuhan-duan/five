package com.five.mapper;

import com.five.pojo.dto.UserRegisterDTO;
import com.five.pojo.entity.User;
import com.five.pojo.vo.UserSelectByIdVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {


    void register(User user);

    @Select("select * from user where " +
            "username=#{username} and password=#{password} and deleted=0")
    User login(UserRegisterDTO userLoginDTO);

    @Select("select * from user where id=#{id} and deleted=0")
    UserSelectByIdVO selectUserById(Long id);

    int update(User user);

    @Select("select * from user where username=#{username} ")
    User selectByUsername(String username);
}
