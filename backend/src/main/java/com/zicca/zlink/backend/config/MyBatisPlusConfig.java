package com.zicca.zlink.backend.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.zicca.zlink.backend.common.enums.EnableStatusEnum;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * MyBatis-Plus 配置类
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 可以添加其他插件，例如分页插件等
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public MyMetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }


    static class MyMetaObjectHandler implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            // fieldName: Java实体类中的属性名
            strictInsertFill(metaObject, "clickNum", () -> 0, Integer.class);
            strictInsertFill(metaObject, "enableStatus", () -> EnableStatusEnum.ENABLE, EnableStatusEnum.class);
            strictInsertFill(metaObject, "totalPv", () -> 0, Integer.class);
            strictInsertFill(metaObject, "totalUv", () -> 0, Integer.class);
            strictInsertFill(metaObject, "totalUip", () -> 0, Integer.class);
            strictInsertFill(metaObject, "deleteTime", () -> 0L, Long.class);
            strictInsertFill(metaObject, "createTime", Date::new, Date.class);
            strictInsertFill(metaObject, "updateTime", Date::new, Date.class);
            strictInsertFill(metaObject, "createBy", () -> "admin", String.class);
            strictInsertFill(metaObject, "updateBy", () -> "admin", String.class);
            strictInsertFill(metaObject, "deleteFlag", () -> 0, Integer.class);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            strictUpdateFill(metaObject, "updateTime", Date::new, Date.class);
            strictUpdateFill(metaObject, "updateBy", () -> "admin", String.class);
        }
    }


}
