package com.example.demo.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponseDTO<T> {

    private final String s;
    private final int ec;
    private final String em;
    private final T d;

    private BaseResponseDTO(String s, int ec, String em, T d) {
        this.s = s;
        this.ec = ec;
        this.em = em;
        this.d = d;
    }

    /** Success: ec=0, em="", d=data */
    public static <T> BaseResponseDTO<T> success(T data) {
        return new BaseResponseDTO<>("ok", 0, "", data);
    }

    /** Error: ec!=0, em=message, d=null (omitted by @JsonInclude) */
    public static <T> BaseResponseDTO<T> error(int ec, String em) {
        return new BaseResponseDTO<>("error", ec, em, null);
    }
}
