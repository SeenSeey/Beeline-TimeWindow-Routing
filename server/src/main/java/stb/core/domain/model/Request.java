package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;
import stb.core.domain.common.GeopositionProvider;
import stb.core.domain.exception.DomainException;

import java.time.Duration;
import java.time.LocalDate;
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
public class Request extends BaseEntity implements GeopositionProvider {

    public static final Duration URGENT_START_LIMIT = Duration.ofMinutes(100);

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

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_equipment_id")
    private Equipment requiredEquipment;

    public Request(String fullAddress,
                   double latitude,
                   double longitude,
                   List<RequestStatus> statuses,
                   JobType jobType,
                   Region region,
                   LocalTime startTimeWindow,
                   LocalTime endTimeWindow) {

        if ((startTimeWindow == null && endTimeWindow != null) || (startTimeWindow != null && endTimeWindow == null)) {
            throw new DomainException.ValidationException("Необходимо указать обе границы временного окна одновременно");
        }

        setFullAddress(fullAddress);
        setLatitude(latitude);
        setLongitude(longitude);
        updateStatus(statuses, RequestStatus.Keyword.IDLE);
        setUrgent(JobType.Keyword.EMERGENCY.equals(jobType.getKeyword()));
        setDeadline(LocalDateTime.now().plusDays(jobType.getDeadlineInDays()));
        setJobType(jobType);
        setRegion(region);
        setCreatedAt(LocalDateTime.now());

        if (startTimeWindow == null) {
            startTimeWindow = LocalTime.of(0, 1);
            endTimeWindow = LocalTime.of(23, 59);
        }

        setStartTimeWindow(startTimeWindow);
        setEndTimeWindow(endTimeWindow);
    }

    public void addVehicle(Vehicle vehicle) {
        setRequiredVehicle(vehicle);
    }

    public void addEquipment(Equipment equipment) {
        setRequiredEquipment(equipment);
    }

    public void assignCrew(EngineerCrew engineerCrew, List<RequestStatus> statuses) {
        setAssignedCrew(engineerCrew);
        setAssignedAt(LocalDateTime.now());
        updateStatus(statuses, RequestStatus.Keyword.ASSIGNED);
    }

    public void unassign(List<RequestStatus> statuses) {
        setAssignedCrew(null);
        setAssignedAt(null);
        if (!isCanceled()) {
            updateStatus(statuses, RequestStatus.Keyword.IDLE);
        }
    }

    public void markInProgress(List<RequestStatus> statuses) {
        if (isCompleted() || isCanceled() || isInProgress()) {
            throw new DomainException.ConflictException("Невозможно начать выполнение заявки в статусе \"%s\"".formatted(status.getDisplayName()));
        }

        if (requiredEquipment != null && assignedCrew == null) {
            throw new DomainException.ConflictException("Невозможно начать выполнение заявки без назначенной бригады");
        }

        if (requiredEquipment != null) {
            assignedCrew.takeEquipment(requiredEquipment);
        }

        setArrivedAt(LocalDateTime.now());
        updateStatus(statuses, RequestStatus.Keyword.IN_PROGRESS);
    }

    public void complete(List<RequestStatus> statuses) {
        if (!isInProgress()) {
            throw new DomainException.ConflictException("Невозможно завершить заявку в статусе \"%s\"".formatted(status.getDisplayName()));
        }

        setCompletedAt(LocalDateTime.now());
        updateStatus(statuses, RequestStatus.Keyword.COMPLETED);
    }

    public void cancel(List<RequestStatus> statuses) {
        if (isInProgress() || isCompleted()) {
            throw new DomainException.ConflictException("Невозможно отменить заявку в статусе \"%s\"".formatted(status.getDisplayName()));
        }

        setCanceledAt(LocalDateTime.now());
        updateStatus(statuses, RequestStatus.Keyword.CANCELED);
    }

    public boolean isAssigned() {
        return assignedAt != null;
    }

    public boolean isInProgress() {
        return arrivedAt != null && !isCompleted();
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public boolean isCanceled() {
        return canceledAt != null;
    }

    private void updateStatus(List<RequestStatus> statuses, RequestStatus.Keyword keyword) {
        setStatus(statuses
                .stream()
                .filter(status -> keyword.equals(status.getKeyword()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Статус %s не найден".formatted(keyword))));
    }
}
