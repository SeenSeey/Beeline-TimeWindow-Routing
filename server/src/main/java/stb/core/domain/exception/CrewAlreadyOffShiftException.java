package stb.core.domain.exception;

public class CrewAlreadyOffShiftException extends DomainException.ConflictException {

    public CrewAlreadyOffShiftException(String message) {
        super(message);
    }
}
