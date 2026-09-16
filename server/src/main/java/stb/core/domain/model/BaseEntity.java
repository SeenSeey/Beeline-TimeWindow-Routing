package stb.core.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@MappedSuperclass
@Getter
@Setter(AccessLevel.PROTECTED)
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false)
    private int id;
}
