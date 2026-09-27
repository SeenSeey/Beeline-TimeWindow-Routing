package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "schedule_slots")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class ScheduleSlot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineer_crew_id")
    private EngineerCrew engineerCrew;

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
                .engineerCrew(slot.getEngineerCrew())
                .type(slot.getType())
                .arrivalTime(slot.getArrivalTime())
                .startTimeWindow(slot.getStartTimeWindow())
                .endTimeWindow(slot.getEndTimeWindow())
                .finishedAt(slot.getFinishedAt())
                .build();
    }

    public ScheduleSlot(Schedule schedule,
                        Request request,
                        EngineerCrew engineerCrew,
                        SlotType type,
                        LocalTime arrivalTime,
                        LocalTime startTimeWindow,
                        LocalTime endTimeWindow) {
        setSchedule(schedule);
        setRequest(request);
        setType(Objects.requireNonNull(type, "type не должен быть null"));
        if (type == SlotType.REQUEST) {
            Objects.requireNonNull(engineerCrew, "engineerCrew обязательна для request-slot");
        }
        setEngineerCrew(engineerCrew);
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
