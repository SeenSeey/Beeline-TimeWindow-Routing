package stb.core.domain.exception;

public class InvalidRequestSearchFiltersException extends DomainException.ValidationException {

    public InvalidRequestSearchFiltersException(String message) {
        super(message);
    }
}
