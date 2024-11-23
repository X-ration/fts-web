package com.adam.ftsweb.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    public static final Pattern EMAIL_PATTERN = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
    public static final Pattern PASSWORD_PATTERN = Pattern.compile("[\\w\\W]+");

    public static boolean isEmail(String email) {
        Objects.requireNonNull(email);
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    public static boolean isPassword(String password) {
        Objects.requireNonNull(password);
        Matcher matcher = PASSWORD_PATTERN.matcher(password);
        return matcher.matches();
    }

    public static void main(String[] args) {
        System.out.println(isPassword("abcd*&#()"));
    }

}
