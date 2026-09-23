package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "requests")
@Getter
@Setter(AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Request extends BaseEntity {

    @Column(name = "full_address", nullable = false)
    private String fullAddress;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id")
    private RequestStatus status;

    @Column(name = "urgent", nullable = false)
    private boolean urgent;

    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_type_id", nullable = false)
    private JobType jobType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "start_time_window", nullable = false)
    private LocalTime startTimeWindow;

    @Column(name = "end_time_window", nullable = false)
    private LocalTime endTimeWindow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_vehicle_id")
    private Vehicle requiredVehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_crew_id")
    private EngineerCrew assignedCrew;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "request_equipment",
            joinColumns =@JoinColumn(name = "request_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private List<Equipment> equipment;

    public void assignCrew(EngineerCrew engineerCrew, List<RequestStatus> statuses) {
        setAssignedCrew(engineerCrew);
        setAssignedAt(LocalDateTime.now());
        setStatus(statuses
                .stream()
                .filter(status -> RequestStatus.Keyword.ASSIGNED.equals(status.getKeyword()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Статус %s не найден".formatted(RequestStatus.Keyword.ASSIGNED))));
    }

    public boolean isAssigned() {
        return assignedAt != null;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public boolean isCanceled() {
        return canceledAt != null;
    }
}
