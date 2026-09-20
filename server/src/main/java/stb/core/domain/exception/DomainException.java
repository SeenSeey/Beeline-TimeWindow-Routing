package stb.core.domain.exception;

import lombok.Getter;

@Getter
public abstract class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String message, String code) {
        super(message);
        this.code = code;
    }

    public static class NotFoundException extends DomainException {

        public NotFoundException(String message) {
            super(message, "NOT_FOUND");
        }
    }

    public static class ConflictException extends DomainException {

        public ConflictException(String message) {
            super(message, "CONFLICT");
        }
    }

    public static class ValidationException extends DomainException {

        public ValidationException(String message) {
            super(message, "VALIDATION_FAILED");
        }
    }

    public static class InternalErrorException extends DomainException {

        public InternalErrorException(String message) {
            super(message, "SERVER_ERROR");
        }
    }
}
