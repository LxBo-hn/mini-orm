package com.t2308e.annotations; // Hoặc package của bạn

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // Dòng này cần Retention và RetentionPolicy
@Target(ElementType.TYPE)        // Dòng này cần Target và ElementType
public @interface MyEntity {
    String tableName() default "";
}