package com.zicca.zlink.framework.web;

import com.zicca.zlink.framework.errorcode.BaseErrorCode;
import com.zicca.zlink.framework.execption.AbstractException;
import com.zicca.zlink.framework.result.Result;

import java.util.Optional;

public final class Results {

    public static Result<Void> success() {
        return new Result<Void>().setCode(Result.SUCCESS_CODE);
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>().setCode(Result.SUCCESS_CODE).setData(data);
    }

    public static Result<Void> fail() {
        return new Result<Void>().setCode(BaseErrorCode.SERVICE_ERROR.code()).setMessage(BaseErrorCode.SERVICE_ERROR.message());
    }

    public static Result<Void> fail(String message) {
        return new Result<Void>().setCode(BaseErrorCode.SERVICE_ERROR.code()).setMessage(BaseErrorCode.SERVICE_ERROR.message());
    }

    protected static Result<Void> fail(AbstractException exception) {
        String errorCode = Optional.ofNullable(exception.getErrorCode()).orElse(BaseErrorCode.SERVICE_ERROR.code());
        String errorMessage = Optional.ofNullable(exception.getErrorMessage()).orElse(BaseErrorCode.SERVICE_ERROR.message());
        return new Result<Void>().setCode(errorCode).setMessage(errorMessage);
    }

    protected static Result<Void> fail(String errorCode, String errorMessage) {
        return new Result<Void>().setCode(errorCode).setMessage(errorMessage);
    }


}
