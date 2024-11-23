package com.adam.ftsweb.controller;

import com.adam.ftsweb.config.WebConfig;
import com.adam.ftsweb.constant.RegisterPageConstant;
import com.adam.ftsweb.dto.RegisterForm;
import com.adam.ftsweb.dto.RegisterFormErrorMsg;
import com.adam.ftsweb.service.UserService;
import com.adam.ftsweb.util.Response;
import com.adam.ftsweb.util.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/loginByFtsId")
    public String loginByFtsId(@RequestParam("ftsId") String ftsIdStr, @RequestParam String password, @RequestParam(required = false) boolean rememberMe, RedirectAttributes redirectAttributes) {
        log.debug("loginByFtsId ftsIdStr={},password={},rememberMe={}", ftsIdStr, password, rememberMe);
        if(StringUtils.isBlank(ftsIdStr) || StringUtils.isBlank(password)) {
            redirectAttributes.addFlashAttribute("error", "无效的请求");
            return "redirect:/user/login";
        }
        int ftsId;
        try {
            ftsId = Integer.parseInt(ftsIdStr);
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "无效的输入");
            return "redirect:/user/login";
        }
        return "redirect:/index";
    }

    @PostMapping("/loginByEmail")
    public String loginByEmail(@RequestParam String email, @RequestParam String password, @RequestParam(required = false) boolean rememberMe, RedirectAttributes redirectAttributes) {
        log.debug("loginByEmail email={},password={},rememberMe={}", email, password, rememberMe);
        if(StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
            redirectAttributes.addFlashAttribute("error", "无效的请求");
            return "redirect:/user/login";
        }
        if(!StringUtil.isEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "无效的输入");
            return "redirect:/user/login";
        }
        return "redirect:/index";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterForm registerForm, Model model) {
        log.debug("register form={}", registerForm);
        Response<RegisterFormErrorMsg> errorObject = checkParamsAndGenerateErrorMsg(registerForm);
        if(!errorObject.isSuccess()) {
            try {
                String json = objectMapper.writeValueAsString(errorObject);
                model.addAttribute("error", json);
            } catch (JsonProcessingException e) {
                log.error("register Jackson processing exception", e);
                model.addAttribute("error", "注册失败：服务器出现异常");
            }
            return "register";
        }
        Response<Long> registerResponse = userService.registerUser(registerForm);
        if(registerResponse.isSuccess()) {
            model.addAttribute("ftsId", registerResponse.getData());
            return "registerSuccess";
        } else {
            model.addAttribute("error", registerResponse.getMessage());
            return "register";
        }
    }

    private Response<RegisterFormErrorMsg> checkParamsAndGenerateErrorMsg(RegisterForm registerForm) {
        RegisterFormErrorMsg errorMsg = new RegisterFormErrorMsg();
        if(StringUtils.isBlank(registerForm.getNickname())) {
            errorMsg.setNickname(RegisterPageConstant.NICKNAME_INPUT_BLANK);
        } else if(registerForm.getNickname().length() > 32) {
            errorMsg.setNickname(RegisterPageConstant.NICKNAME_LENGTH_EXCEEDED);
        }
        if(StringUtils.isBlank(registerForm.getEmail())) {
            errorMsg.setEmail(RegisterPageConstant.EMAIL_INPUT_BLANK);
        } else if(registerForm.getEmail().length() > 256) {
            errorMsg.setEmail(RegisterPageConstant.EMAIL_LENGTH_EXCEEDED);
        } else if(!StringUtil.isEmail(registerForm.getEmail())) {
            errorMsg.setEmail(RegisterPageConstant.EMAIL_INVALID);
        }
        if(StringUtils.isBlank(registerForm.getPassword())) {
            errorMsg.setPassword(RegisterPageConstant.PASSWORD_INPUT_BLANK);
        } else if(registerForm.getPassword().length() > 32) {
            errorMsg.setPassword(RegisterPageConstant.PASSWORD_LENGTH_EXCEEDED);
        } else if(!StringUtil.isPassword(registerForm.getPassword())) {
            errorMsg.setPassword(RegisterPageConstant.PASSWORD_INVALID);
        }
        if(StringUtils.isBlank(registerForm.getVerifyPassword())) {
            errorMsg.setVerifyPassword(RegisterPageConstant.VERIFY_PASSWORD_INPUT_BLANK);
        } else if(!StringUtils.equals(registerForm.getPassword(), registerForm.getVerifyPassword())) {
            errorMsg.setVerifyPassword(RegisterPageConstant.VERIFY_PASSWORD_NOT_EQUAL);
        }
        if(!StringUtils.isBlank(registerForm.getBirthDate())) {
            try {
                LocalDate localDate = LocalDate.parse(registerForm.getBirthDate(), WebConfig.DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                log.error("checkParams parsing birthdate exception", e);
                errorMsg.setBirthDate(RegisterPageConstant.BIRTHDATE_INVALID);
            }
        }
        if(registerForm.getHobby().length() > 100) {
            errorMsg.setHobby(RegisterPageConstant.HOBBY_LENGTH_EXCEEDED);
        }
        if(registerForm.getAutograph().length() > 100) {
            errorMsg.setAutograph(RegisterPageConstant.AUTOGRAPH_LENGTH_EXCEEDED);
        }

        if(errorMsg.getNickname() != null || errorMsg.getEmail() != null || errorMsg.getPassword() != null || errorMsg.getVerifyPassword() != null
                || errorMsg.getBirthDate() != null || errorMsg.getHobby() != null || errorMsg.getAutograph() != null) {
            return Response.fail("注册失败，请检查输入！", errorMsg);
        } else {
            return Response.success();
        }
    }

}
