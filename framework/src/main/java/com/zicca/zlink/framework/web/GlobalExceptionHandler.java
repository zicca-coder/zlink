package com.zicca.zlink.framework.web;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.zicca.zlink.framework.errorcode.BaseErrorCode;
import com.zicca.zlink.framework.execption.AbstractException;
import com.zicca.zlink.framework.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@Slf4j(topic = "GlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 拦截参数验证异常
     */
    @SneakyThrows
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result validExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        FieldError firstFieldError = CollectionUtil.getFirst(bindingResult.getFieldErrors());
        String exceptionStr = Optional.ofNullable(firstFieldError)
                .map(FieldError::getDefaultMessage)
                .orElse(StrUtil.EMPTY);
        log.error("[{}] {} [ex] {}", request.getMethod(), getUrl(request), exceptionStr);
        return Results.fail(BaseErrorCode.CLIENT_ERROR.code(), exceptionStr);
    }

    /**
     * 拦截应用内抛出的异常
     */
    @ExceptionHandler(value = {AbstractException.class})
    public Result abstractException(HttpServletRequest request, AbstractException ex) {
        if (ex.getCause() != null) {
            log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex, ex.getCause());
            return Results.fail(ex);
        }
        StringBuilder stackTraceBuilder = new StringBuilder();
        stackTraceBuilder.append(ex.getClass().getName()).append(": ").append(ex.getErrorMessage()).append("\n");
        StackTraceElement[] stackTrace = ex.getStackTrace();
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
            stackTraceBuilder.append("\tat ").append(stackTrace[i]).append("\n");
        }
        log.error("[{}] {} [ex] {} \n\n{}", request.getMethod(), request.getRequestURL().toString(), ex, stackTraceBuilder);
        return Results.fail(ex);
    }

    /**
     * 拦截未捕获异常
     */
    @ExceptionHandler(value = Throwable.class)
    public Result defaultErrorHandler(HttpServletRequest request, Throwable throwable) {
        log.error("[{}] {} ", request.getMethod(), getUrl(request), throwable);
        return Results.fail();
    }

    private String getUrl(HttpServletRequest request) {
        if (StringUtils.isEmpty(request.getQueryString())) {
            return request.getRequestURL().toString();
        }
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }


}
