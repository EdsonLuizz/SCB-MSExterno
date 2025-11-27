package com.scb.externo.exception;

public class MailgunException extends RuntimeException {
    public MailgunException(String message) {
        super(message);
    }

    public MailgunException(String message, Throwable cause) {
        super(message, cause);
    }
}