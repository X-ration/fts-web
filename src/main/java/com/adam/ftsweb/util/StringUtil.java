package com.adam.ftsweb.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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

    public static String generatePasswordSalt() {
        return generate32digitRandomUUID();
    }

    public static String generate32digitRandomUUID() {
        return UUID.randomUUID().toString().replace("-","");
    }

    public static String encryptPasswordMD5(String password, String salt) {
        Assert.isTrue(StringUtils.isNoneBlank(password, salt), "encryptPasswordMD5 param blank");
        return DigestUtils.md5DigestAsHex((salt + password).getBytes());
    }

    public static boolean checkPasswordMD5(String password, String encryptedPassword, String salt) {
        Assert.isTrue(StringUtils.isNoneBlank(password, encryptedPassword, salt), "checkPasswordMD5 param blank");
        String encrypted = DigestUtils.md5DigestAsHex((salt + password).getBytes());
        return StringUtils.equals(encrypted, encryptedPassword);
    }

    public static void main(String[] args) {
        String salt = generatePasswordSalt();
        String password = "123456";
        String encryptedPassword = encryptPasswordMD5(password, salt);
        System.out.println(encryptedPassword + "," + encryptedPassword.length());
        System.out.println(checkPasswordMD5("123456", encryptedPassword, salt));
    }

}
