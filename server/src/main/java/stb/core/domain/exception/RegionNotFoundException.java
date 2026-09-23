package stb.core.domain.exception;

public class RegionNotFoundException extends DomainException.NotFoundException {

    public RegionNotFoundException(String message) {
        super(message);
    }
}
