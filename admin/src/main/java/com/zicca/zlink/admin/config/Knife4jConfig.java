package com.zicca.zlink.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 配置类
 */
@Configuration
@Slf4j(topic = "Knife4jConfig")
public class Knife4jConfig implements CommandLineRunner {

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("z-link后端API文档")
                        .description("zlink后端API文档，提供短链接生成、解析等功能")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("zicca")
                                .email("zqianglee@outlook.com")
                                .url("https://gitcode.com/zicca/zlink"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Knife4j在线文档地址 http://localhost:{}/doc.html", serverPort);
    }
}
