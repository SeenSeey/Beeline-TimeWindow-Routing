package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skills")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Skill extends BaseEntity {

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "keyword", nullable = false)
    @Enumerated(EnumType.STRING)
    private Keyword keyword;

    public enum Keyword {

        LOCAL_WORK,
        INSTALLATION_OR_ADDON,
        EMERGENCY
    }
}
