package com.zicca.zlink.framework.execption;

import com.zicca.zlink.framework.errorcode.BaseErrorCode;
import com.zicca.zlink.framework.errorcode.IErrorCode;

public class ClientException extends AbstractException {

    public ClientException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ClientException(String errorMessage) {
        this(errorMessage, null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String errorMessage, IErrorCode errorCode) {
        this(errorMessage, null, errorCode);
    }


    public ClientException(String errorMessage, Throwable throwable, IErrorCode errorCode) {
        super(errorMessage, throwable, errorCode);
    }

    @Override
    public String toString() {
        return "ClientException{" +
                "code=" + getErrorCode() +
                ", message='" + getErrorMessage() + '\'' +
                '}';
    }
}
