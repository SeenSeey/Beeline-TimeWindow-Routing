package stb.core.domain.exception;

public class JobTypeNotFoundException extends DomainException.NotFoundException {

    public JobTypeNotFoundException(String message) {
        super(message);
    }
}
