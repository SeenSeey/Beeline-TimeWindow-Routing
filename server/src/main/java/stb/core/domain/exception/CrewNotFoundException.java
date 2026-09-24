package stb.core.domain.exception;

public class CrewNotFoundException extends DomainException.NotFoundException {

    public CrewNotFoundException(String message) {
        super(message);
    }
}
