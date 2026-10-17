package com.pasalapeli.notification.exception;

/** Error de negocio / validacion de solicitudes de administracion (HTTP 400). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
