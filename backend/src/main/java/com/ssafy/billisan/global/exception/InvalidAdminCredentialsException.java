package com.ssafy.billisan.global.exception;

public class InvalidAdminCredentialsException extends RuntimeException {
    public InvalidAdminCredentialsException(String message) {
        super(message);
    }
}
