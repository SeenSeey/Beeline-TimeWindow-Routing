package stb.core.domain.exception;

public class RequestNotFoundException extends DomainException.NotFoundException {

    public RequestNotFoundException(String message) {
        super(message);
    }
}
