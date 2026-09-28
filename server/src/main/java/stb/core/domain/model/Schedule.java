package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schedules", uniqueConstraints = @UniqueConstraint(
        name = "uk_schedules_previous_schedule",
        columnNames = "previous_schedule_id"
))
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "previous_schedule_id")
    private Schedule previousSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "schedule", cascade = CascadeType.ALL)
    private List<ScheduleSlot> slots;

    public static Schedule createFrom(Schedule schedule) {
        return Schedule.builder()
                .targetDate(schedule.getTargetDate())
                .createdAt(LocalDateTime.now())
                .status(Status.CREATED)
                .previousSchedule(schedule)
                .region(schedule.getRegion())
                .slots(new ArrayList<>())
                .finishedAt(schedule.getFinishedAt())
                .build();
    }

    public Schedule(LocalDate targetDate, Region region) {
        setTargetDate(targetDate);
        setRegion(region);
        setStatus(Status.CALCULATING);
        setCreatedAt(LocalDateTime.now());
        setFinishedAt(LocalDateTime.now());
        setSlots(new ArrayList<>());
    }

    public ScheduleSlot addSlot(Request request,
                        EngineerCrew engineerCrew,
                        ScheduleSlot.SlotType type,
                        LocalTime arrivalTime,
                        LocalTime startTimeWindow,
                        LocalTime endTimeWindow) {

        var slot = new ScheduleSlot(this, request, engineerCrew, type, arrivalTime, startTimeWindow, endTimeWindow);
        slots.add(slot);

        return slot;
    }

    public ScheduleSlot addSlot(ScheduleSlot slot) {
        ScheduleSlot slotCopy = ScheduleSlot.createFrom(this, slot);
        slots.add(slotCopy);

        return slotCopy;
    }

    public void markAsRecalculating() {
        setStatus(Status.RECALCULATING);
    }

    public void markAsCreated() {
        setStatus(Status.CREATED);
    }

    public enum Status {
        CALCULATING("Формируется"),
        RECALCULATING("Переформирование"),
        CREATED("Готово");

        @Getter
        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }
    }
}
