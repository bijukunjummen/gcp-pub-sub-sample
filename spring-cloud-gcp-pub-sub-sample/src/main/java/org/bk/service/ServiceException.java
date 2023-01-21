package org.bk.service;

public class ServiceException extends RuntimeException {
    public ServiceException(Exception base) {
        super(base);
    }
}
