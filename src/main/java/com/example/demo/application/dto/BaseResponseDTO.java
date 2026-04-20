package com.example.demo.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class BaseResponseDTO<T> {
    private String ec;
    private String em;
    private T data;

    public BaseResponseDTO(String ec, String em, T data) {
        this.ec = ec;
        this.em = em;
        this.data = data;
    }

    public static <T> BaseResponseDTO<T> success(T data) {
        return new BaseResponseDTO<>("0", "SUCCESS", data);
    }

    public static <T> BaseResponseDTO<T> error(String ec, String em) {
        return new BaseResponseDTO<>(ec, em, null);
    }

}
