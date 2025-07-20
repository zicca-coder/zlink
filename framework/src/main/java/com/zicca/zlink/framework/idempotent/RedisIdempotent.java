package com.zicca.zlink.framework.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisIdempotent {

    /**
     * SpEL表达式生成唯一key
     */
    String key();

    /**
     * key前缀
     */
    String keyPrefix() default "hp:idempotent:";

    /**
     * 过期时间（秒）
     */
    long timeout() default 3600L;
}
