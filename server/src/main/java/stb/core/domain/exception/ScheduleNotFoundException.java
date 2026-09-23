package stb.core.domain.exception;

public class ScheduleNotFoundException extends DomainException.NotFoundException {

    public ScheduleNotFoundException(String message) {
        super(message);
    }
}
