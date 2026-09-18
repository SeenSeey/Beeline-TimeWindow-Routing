package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_slots")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSlot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SlotType type;

    @Column(name = "start_time_window", nullable = false)
    private LocalTime startTimeWindow;

    @Column(name = "end_time_window", nullable = false)
    private LocalTime endTimeWindow;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public enum SlotType {

        REQUEST,
        TRAVEL,
        BREAK
    }
}
