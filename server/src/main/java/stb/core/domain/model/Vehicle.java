package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehicle extends BaseEntity {

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "keyword", nullable = false)
    @Enumerated(EnumType.STRING)
    private Keyword keyword;

    @Column(name = "priority", nullable = false)
    private int priority;

    public enum Keyword {

        PEDESTRIAN,
        BICYCLE,
        PUBLIC_TRANSPORT,
        CAR
    }
}
