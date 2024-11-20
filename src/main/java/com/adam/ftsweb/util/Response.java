package com.adam.ftsweb.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Controller返回的响应体类型
 * @param <T>
 */
@Data
@AllArgsConstructor
public class Response<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> Response<T> success() {
        return new Response<>(true, null, null);
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(true, null, data);
    }

    public static <T> Response<T> fail() {
        return new Response<>(false, null, null);
    }

    public static <T> Response<T> fail(String message) {
        return new Response<>(false, message, null);
    }

    public static <T> Response<T> fail(String message, T data) {
        return new Response<>(false, message, data);
    }

    public static void main(String[] args) {
        System.out.println(success());
    }
}
