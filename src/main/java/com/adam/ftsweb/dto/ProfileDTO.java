package com.adam.ftsweb.dto;

import lombok.Data;

@Data
public class ProfileDTO {
    private long ftsId;
    private String nickname;
    private String email;
    private String birthDate;
    private String hobby;
    private String autograph;
}
