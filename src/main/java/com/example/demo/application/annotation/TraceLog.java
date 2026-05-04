package com.example.demo.application.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceLog {
    String value() default ""; // Tùy chọn để ghi chú thêm (ví dụ: tên operation)
}
