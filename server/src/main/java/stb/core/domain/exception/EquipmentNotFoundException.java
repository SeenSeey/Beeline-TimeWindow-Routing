package stb.core.domain.exception;

public class EquipmentNotFoundException extends DomainException.NotFoundException {

    public EquipmentNotFoundException(String message) {
        super(message);
    }
}
