package stb.core.domain.exception;

public class ScheduleAlreadyExistsException extends DomainException.ConflictException {

    public ScheduleAlreadyExistsException(String message) {
        super(message);
    }
}
