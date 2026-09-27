package stb.core.domain.exception;

public class VehicleNotFoundException extends DomainException.NotFoundException {

    public VehicleNotFoundException(String message) {
        super(message);
    }
}
