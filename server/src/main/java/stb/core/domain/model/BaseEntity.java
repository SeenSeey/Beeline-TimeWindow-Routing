package stb.core.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @Id
    @AssignedOrIdentity
    @Column(name = "id", nullable = false)
    private int id;
}
