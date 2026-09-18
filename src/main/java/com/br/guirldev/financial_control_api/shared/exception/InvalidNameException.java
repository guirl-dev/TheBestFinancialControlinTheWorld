package com.br.guirldev.financial_control_api.shared.exception;

public class InvalidNameException extends IllegalArgumentException {
    public InvalidNameException(String message) {
        super(message);
    }
}
