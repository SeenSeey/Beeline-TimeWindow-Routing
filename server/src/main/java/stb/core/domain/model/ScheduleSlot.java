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
@Builder(access = AccessLevel.PROTECTED)
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

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "start_time_window")
    private LocalTime startTimeWindow;

    @Column(name = "end_time_window")
    private LocalTime endTimeWindow;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public static ScheduleSlot createFrom(Schedule schedule, ScheduleSlot slot) {
        return ScheduleSlot.builder()
                .schedule(schedule)
                .request(slot.getRequest())
                .type(slot.getType())
                .arrivalTime(slot.getArrivalTime())
                .startTimeWindow(slot.getStartTimeWindow())
                .endTimeWindow(slot.getEndTimeWindow())
                .finishedAt(slot.getFinishedAt())
                .build();
    }

    public ScheduleSlot(Schedule schedule, Request request, SlotType type, LocalTime arrivalTime, LocalTime startTimeWindow, LocalTime endTimeWindow) {
        setSchedule(schedule);
        setRequest(request);
        setType(type);
        setArrivalTime(arrivalTime);
        setStartTimeWindow(startTimeWindow);
        setEndTimeWindow(endTimeWindow);
    }

    public enum SlotType {

        REQUEST,
        TRAVEL,
        BREAK
    }
}
