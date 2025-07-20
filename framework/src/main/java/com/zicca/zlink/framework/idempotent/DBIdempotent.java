package com.zicca.zlink.framework.idempotent;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DBIdempotent {

    /**
     * 通过 SpEL 表达式生成的唯一 Key
     */
    String key();
}
