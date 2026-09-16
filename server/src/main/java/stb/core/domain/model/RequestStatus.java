package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "request_statuses")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestStatus extends BaseEntity {

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "keyword", nullable = false)
    @Enumerated(EnumType.STRING)
    private Keyword keyword;

    public enum Keyword {
        IDLE,
        ASSIGNED,

        // FIXME: делаем ли подтверждение, что бригада на адресе?
        IN_PROGRESS,

        COMPLETED,
        CANCELED
    }
}
