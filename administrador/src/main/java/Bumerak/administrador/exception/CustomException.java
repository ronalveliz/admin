package Bumerak.administrador.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException  {
    private final HttpStatus status;
    private final String message;

    public CustomException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.message = message;
    }

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.message = message;
    }
}

