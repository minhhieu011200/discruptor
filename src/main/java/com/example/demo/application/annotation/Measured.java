package com.example.demo.application.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // Áp dụng cho method
@Retention(RetentionPolicy.RUNTIME) // Giữ annotation đến runtime
public @interface Measured {
    String value() default ""; // Tên metric (mặc định là tên method)

    String description() default "";
}