package com.adam.ftsweb.controller;

import com.adam.ftsweb.mapper.UserMapper;
import com.adam.ftsweb.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequestMapping("/")
public class HelloController {

    @Autowired
    private UserMapper userMapper;

    @RequestMapping("hello")
    @ResponseBody
    public Response<?> hello() {
        return Response.success();
    }

    @RequestMapping("helloPage")
    public String helloPage() {
        return "hello";
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
        long maxFtsId = userMapper.queryMaxFtsId();
        return Response.success(maxFtsId);
    }

}