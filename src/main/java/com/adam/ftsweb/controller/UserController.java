package com.adam.ftsweb.controller;

import com.adam.ftsweb.config.WebConfig;
import com.adam.ftsweb.constant.LoginPageConstant;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private HttpServletResponse response;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/loginByFtsId")
    public String loginByFtsId(@RequestParam("ftsId") String ftsIdStr, @RequestParam String password, @RequestParam(required = false) boolean rememberMe, RedirectAttributes redirectAttributes) {
        log.debug("loginByFtsId ftsIdStr={},password={},rememberMe={}", ftsIdStr, password, rememberMe);
        if(StringUtils.isBlank(ftsIdStr) || StringUtils.isBlank(password)) {
            redirectAttributes.addFlashAttribute("error", LoginPageConstant.INVALID_REQUEST);
            return "redirect:/user/login";
        }
        int ftsId;
        try {
            ftsId = Integer.parseInt(ftsIdStr);
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", LoginPageConstant.INPUT_INVALID);
            return "redirect:/user/login";
        }
        Response<Long> loginResponse = userService.loginByFtsId(ftsId, password, rememberMe, request.getSession(), response);
        if(loginResponse.isSuccess()) {
            return "redirect:/index";
        } else {
            redirectAttributes.addFlashAttribute("error", loginResponse.getMessage());
            return "redirect:/user/login";
        }
    }

    @PostMapping("/loginByEmail")
    public String loginByEmail(@RequestParam String email, @RequestParam String password, @RequestParam(required = false) boolean rememberMe, RedirectAttributes redirectAttributes) {
        log.debug("loginByEmail email={},password={},rememberMe={}", email, password, rememberMe);
        if(StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
            redirectAttributes.addFlashAttribute("error", LoginPageConstant.INVALID_REQUEST);
            return "redirect:/user/login";
        }
        if(email.length() > 256) {
            redirectAttributes.addFlashAttribute("error", LoginPageConstant.EMAIL_LENGTH_EXCEEDED);
            return "redirect:/user/login";
        }
        if(!StringUtil.isEmail(email)) {
            redirectAttributes.addFlashAttribute("error", LoginPageConstant.EMAIL_INVALID);
            return "redirect:/user/login";
        }
        Response<Long> loginResponse = userService.loginByEmail(email, password, rememberMe, request.getSession(), response);
        if(loginResponse.isSuccess()) {
            return "redirect:/index";
        } else {
            redirectAttributes.addFlashAttribute("error", loginResponse.getMessage());
            return "redirect:/user/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterForm registerForm, Model model) {
        log.debug("register form={}", registerForm);
        RegisterFormErrorMsg errorObject = checkParamsAndGenerateErrorMsg(registerForm, true);
        if(errorObject.hasErrors()) {
            try {
                Response<RegisterFormErrorMsg> errorMsgResponse = Response.fail("参数校验不通过", errorObject);
                String json = objectMapper.writeValueAsString(errorMsgResponse);
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

    public RegisterFormErrorMsg checkParamsAndGenerateErrorMsg(RegisterForm registerForm, boolean checkEmail) {
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
        } else if(checkEmail && userService.userExistsByEmail(registerForm.getEmail())) {
            errorMsg.setEmail(RegisterPageConstant.EMAIL_ALREADY_IN_USE);
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
        if(registerForm.getHobby() != null && registerForm.getHobby().length() > 100) {
            errorMsg.setHobby(RegisterPageConstant.HOBBY_LENGTH_EXCEEDED);
        }
        if(registerForm.getAutograph() != null && registerForm.getAutograph().length() > 100) {
            errorMsg.setAutograph(RegisterPageConstant.AUTOGRAPH_LENGTH_EXCEEDED);
        }
        return errorMsg;
    }

}
