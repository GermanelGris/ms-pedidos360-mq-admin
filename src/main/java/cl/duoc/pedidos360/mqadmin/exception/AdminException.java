package cl.duoc.pedidos360.mqadmin.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio del administrador, con el código HTTP con que se responde. */
public class AdminException extends RuntimeException {

    private final HttpStatus status;

    public AdminException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AdminException notFound(String message) {
        return new AdminException(HttpStatus.NOT_FOUND, message);
    }

    public static AdminException conflict(String message) {
        return new AdminException(HttpStatus.CONFLICT, message);
    }

    public static AdminException invalid(String message) {
        return new AdminException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
