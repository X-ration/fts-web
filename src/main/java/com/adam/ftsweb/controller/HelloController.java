package com.adam.ftsweb.controller;

import com.adam.ftsweb.common.UserTokenMapItem;
import com.adam.ftsweb.dto.WebSocketLeftMessage;
import com.adam.ftsweb.dto.WebSocketMainMessage;
import com.adam.ftsweb.mapper.FriendRelationshipMapper;
import com.adam.ftsweb.mapper.UserMapper;
import com.adam.ftsweb.po.FriendRelationship;
import com.adam.ftsweb.po.User;
import com.adam.ftsweb.service.FriendRelationshipService;
import com.adam.ftsweb.service.MessageService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class HelloController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FriendRelationshipMapper friendRelationshipMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private FriendRelationshipService friendRelationshipService;
    @Autowired
    private MessageService messageService;

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

    @RequestMapping("helloInitialMessageList")
    @ResponseBody
    public Response<?> helloInitialMessageList() {
        List<WebSocketLeftMessage> webSocketLeftMessageList = messageService.queryMessageListByFtsId(10000);
        return Response.success(webSocketLeftMessageList);
    }

    @RequestMapping("helloQueryMessageListByTwoFtsIds")
    @ResponseBody
    public Response<?> helloQueryMessageListByTwoFtsIds() {
        Map<String, Object> map = new HashMap<>();
        List<WebSocketMainMessage> mainMessageList = messageService.queryMessageListByTwoFtsIds(10000, 10001);
        map.put("10000-10001", mainMessageList);
        mainMessageList = messageService.queryMessageListByTwoFtsIds(10000,10002);
        map.put("10000-10002", mainMessageList);
        return Response.success(map);
    }

}