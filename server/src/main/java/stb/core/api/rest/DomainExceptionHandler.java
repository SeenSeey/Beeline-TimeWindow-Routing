package stb.core.api.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import stb.core.api.rest.v1.dto.ServerResponse;
import stb.core.domain.exception.DomainException;

@Slf4j
@RestControllerAdvice
public class DomainExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServerResponse<?>> handleNotFound(Exception exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        String code = "SERVER_ERROR";
        String message = "Внутренняя ошибка сервера";

        if (exception instanceof DomainException domainException) {
            code = domainException.getCode();
            message = domainException.getMessage();

            status = switch (domainException) {
                case DomainException.NotFoundException _ -> HttpStatus.NOT_FOUND;
                case DomainException.ConflictException _ -> HttpStatus.CONFLICT;
                case DomainException.ValidationException _ -> HttpStatus.BAD_REQUEST;
                default -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
        }

        log.error(message, exception);

        ServerResponse<?> response = ServerResponse.error(status.value(), code, message);
        return new ResponseEntity<>(response, status);
    }

}

