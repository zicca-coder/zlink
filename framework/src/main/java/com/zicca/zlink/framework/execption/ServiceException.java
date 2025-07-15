package com.zicca.zlink.framework.execption;

import com.zicca.zlink.framework.errorcode.BaseErrorCode;
import com.zicca.zlink.framework.errorcode.IErrorCode;

public class ServiceException extends AbstractException {

    public ServiceException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ServiceException(String errorMessage) {
        this(errorMessage, null, BaseErrorCode.SERVICE_ERROR);
    }

    public ServiceException(String errorMessage, IErrorCode errorCode) {
        this(errorMessage, null, errorCode);
    }

    public ServiceException(String errorMessage, Throwable throwable, IErrorCode errorCode) {
        super(errorMessage, throwable, errorCode);
    }

    @Override
    public String toString() {
        return "ServiceException{" +
                "code=" + getErrorCode() +
                ", message='" + getErrorMessage() + '\'' +
                '}';
    }
}
