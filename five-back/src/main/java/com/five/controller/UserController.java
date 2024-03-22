package com.five.controller;

import com.five.mapper.UserMapper;
import com.five.pojo.dto.UserRegisterDTO;
import com.five.pojo.pojo.Result;
import com.five.pojo.vo.UserLoginVO;
import com.five.pojo.vo.UserSelectByIdVO;
import com.five.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/register")
    public Result<Object> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        log.info("User registration, registration details: {}", userRegisterDTO);
        userService.register(userRegisterDTO);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserRegisterDTO userLoginDTO) {
        log.info("User login, login details: {}", userLoginDTO);
        UserLoginVO userLoginVO = userService.login(userLoginDTO);
        return Result.success(userLoginVO);
    }


    @GetMapping("/{id}")
    public Result<UserSelectByIdVO> getById(@PathVariable Long id) {
        log.info("Query user information by id, id: {}", id);
        UserSelectByIdVO userSelectByIdVO = userService.getById(id);
        log.info("User information queried: {}", userSelectByIdVO);
        return Result.success(userSelectByIdVO);
    }


    @DeleteMapping("/{id}")
    public Result<Object> deleteById( @PathVariable Long id) {
        log.info("User delete, id: {}", id);
        userService.deleteById(id);
        return Result.success();
    }

}
