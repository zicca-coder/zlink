package com.zicca.zlink.framework.execption;

import com.zicca.zlink.framework.errorcode.BaseErrorCode;
import com.zicca.zlink.framework.errorcode.IErrorCode;

public class RemoteException extends AbstractException {

    public RemoteException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public RemoteException(String errorMessage) {
        this(errorMessage, null, BaseErrorCode.REMOTE_ERROR);
    }

    public RemoteException(String errorMessage, IErrorCode errorCode) {
        this(errorMessage, null, errorCode);
    }

    public RemoteException(String errorMessage, Throwable throwable, IErrorCode errorCode) {
        super(errorMessage, throwable, errorCode);
    }

    @Override
    public String toString() {
        return "RemoteException{" +
                "code=" + getErrorCode() +
                ", message='" + getErrorMessage() + '\'' +
                '}';
    }
}
