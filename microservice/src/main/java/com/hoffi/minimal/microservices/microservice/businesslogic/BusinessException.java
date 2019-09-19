package com.hoffi.minimal.microservices.microservice.businesslogic;

public class BusinessException extends Exception {
    private static final long serialVersionUID = 1L;

    public BusinessException() {
    }

    public BusinessException(String arg0) {
        super(arg0);
    }

    public BusinessException(Throwable arg0) {
        super(arg0);
    }

    public BusinessException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public BusinessException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
        super(arg0, arg1, arg2, arg3);
    }

}
