package com.adam.ftsweb.controller;

import com.adam.ftsweb.common.UserTokenMapItem;
import com.adam.ftsweb.mapper.FriendRelationshipMapper;
import com.adam.ftsweb.mapper.UserMapper;
import com.adam.ftsweb.po.FriendRelationship;
import com.adam.ftsweb.po.User;
import com.adam.ftsweb.service.UserService;
import com.adam.ftsweb.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/")
public class HelloController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FriendRelationshipMapper friendRelationshipMapper;
    @Autowired
    private UserService userService;

    @RequestMapping("hello")
    @ResponseBody
    public Response<?> hello() {
        return Response.success();
    }

    @RequestMapping("helloPage")
    public String helloPage() {
        return "hello";
    }

    /**
     * 仅供测试
     * @return
     */
    @RequestMapping("helloIndexPage")
    public String helloIndexPage() {
        return "index";
    }

    @RequestMapping("helloDate")
    @ResponseBody
    public Response<?> helloDate() {
        LocalDate localDate = LocalDate.now();
        return Response.success(localDate);
    }

    @RequestMapping("helloDateTime")
    @ResponseBody
    public Response<?> helloDateTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return Response.success(localDateTime);
    }

    @RequestMapping("helloTime")
    @ResponseBody
    public Response<?> helloTime() {
        LocalTime localTime = LocalTime.now();
        return Response.success(localTime);
    }

    @RequestMapping("helloMapper")
    @ResponseBody
    public Response<?> helloMapper() {
        User user = userMapper.queryUserByFtsId(10000);
        return Response.success(user);
    }

    @RequestMapping("helloFriendRelationshipMapper")
    @ResponseBody
    public Response<?> helloFriendRelationshipMapper() {
        FriendRelationship friendRelationship = new FriendRelationship();
        friendRelationship.setUserFtsId(1);
        friendRelationship.setAnotherUserFtsId(2);
        friendRelationship.setAddType(FriendRelationship.FriendRelationshipAddType.web);
        friendRelationshipMapper.insertFriendRelationship(friendRelationship);
        return Response.success(friendRelationship);
    }

    @RequestMapping("helloToken")
    @ResponseBody
    public Response<?> helloToken(@RequestParam String token) {
        UserTokenMapItem item = userService.getUserTokenMapItem(token);
        return Response.success(item);
    }

}