package com.five.service;

import com.five.pojo.dto.UserRegisterDTO;
import com.five.pojo.vo.UserLoginVO;
import com.five.pojo.vo.UserSelectByIdVO;


public interface UserService {

    void register(UserRegisterDTO userRegisterDTO);

    UserLoginVO login(UserRegisterDTO userLoginDTO);

    UserSelectByIdVO getById(Long id);

    void deleteById(Long id);
}
