package com.adam.ftsweb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class RegisterFormErrorMsg {

    private String nickname;
    private String email;
    private String password;
    private String verifyPassword;
    private String birthDate;
    private String hobby;
    private String autograph;

}
