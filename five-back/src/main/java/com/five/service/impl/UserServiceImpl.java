package com.five.service.impl;

import com.five.constant.UserConstant;
import com.five.exception.UserException;
import com.five.mapper.UserMapper;
import com.five.pojo.dto.UserRegisterDTO;
import com.five.pojo.entity.User;
import com.five.pojo.pojo.JwtProperties;
import com.five.pojo.vo.UserLoginVO;
import com.five.pojo.vo.UserSelectByIdVO;
import com.five.service.UserService;
import com.five.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;


    @Override
    @Transactional
    public void register(UserRegisterDTO userRegisterDTO) {
        // Check if there is a similar username in the database
        User existingUser = userMapper.selectByUsername(userRegisterDTO.getUsername());

        // Username already exists
        if (existingUser != null) {
            throw new UserException(UserConstant.USERNAME_DUPLICATE);
        }

        // Create a new User object and set username and password from UserRegisterDTO
        User newUser = new User();
        newUser.setUsername(userRegisterDTO.getUsername());
        newUser.setPassword(userRegisterDTO.getPassword());

        userMapper.register(newUser);
    }

    @Override
    @Transactional
    public UserLoginVO login(UserRegisterDTO userLoginDTO) {
       
        User user = userMapper.login(userLoginDTO);
        log.info("User information queried: {}", user);
        // No data found
        if (user == null) {
            throw new UserException(UserConstant.USERNAME_OR_PASSWORD_WRONG);
        }
        // Generate JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        String jwt = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims);

        // Assemble data
        // First copy some data
        UserLoginVO userLoginVO = new UserLoginVO();
        BeanUtils.copyProperties(user, userLoginVO);

        // Add JWT token
        userLoginVO.setJwt(jwt);
        log.info("User information queried: {}", userLoginVO);

        return userLoginVO;
    }

    @Override
    public UserSelectByIdVO getById(Long id) {
        return userMapper.selectUserById(id);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        User user = User.builder().id(id).deleted(1).build();
        // Call conditional update
        int deleted = userMapper.update(user);
        // Update failed
        if (deleted == 0) {
            throw new UserException(UserConstant.DELETE_FAILED);
        }
    }
}
