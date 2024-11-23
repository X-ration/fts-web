package com.adam.ftsweb.dto;

import lombok.Data;

@Data
public class RegisterForm {

    private String nickname;
    private String email;
    private String password;
    private String verifyPassword;
    private String birthDate;
    private String hobby;
    private String autograph;

}
