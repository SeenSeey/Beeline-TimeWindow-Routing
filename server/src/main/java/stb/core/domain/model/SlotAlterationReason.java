package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "slot_alteration_reasons", indexes = @Index(
        name = "idx_slot_alteration_reasons_slot_action",
        columnList = "slot_id, action"
))
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SlotAlterationReason extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private ScheduleSlot slot;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private Action action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cause_request_id")
    private Request causeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cause_crew_id")
    private EngineerCrew causeCrew;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static SlotAlterationReason causedByRequest(ScheduleSlot slot,
                                                       Action action,
                                                       Request causeRequest,
                                                       LocalDateTime createdAt) {
        if (!action.isRequestCause()) {
            throw new IllegalArgumentException("Action должен описывать request cause");
        }
        return new SlotAlterationReason(slot, action,
                Objects.requireNonNull(causeRequest, "causeRequest не должна быть null"), null, createdAt);
    }

    public static SlotAlterationReason causedByCrew(ScheduleSlot slot,
                                                    Action action,
                                                    EngineerCrew causeCrew,
                                                    LocalDateTime createdAt) {
        if (!action.isCrewCause()) {
            throw new IllegalArgumentException("Action должен описывать crew cause");
        }
        return new SlotAlterationReason(slot, action, null,
                Objects.requireNonNull(causeCrew, "causeCrew не должна быть null"), createdAt);
    }

    private SlotAlterationReason(ScheduleSlot slot,
                                 Action action,
                                 Request causeRequest,
                                 EngineerCrew causeCrew,
                                 LocalDateTime createdAt) {
        this.slot = Objects.requireNonNull(slot, "slot не должен быть null");
        this.action = Objects.requireNonNull(action, "action не должен быть null");
        this.causeRequest = causeRequest;
        this.causeCrew = causeCrew;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt не должен быть null");
    }

    public enum Action {
        ADDED_FOR_HIGHER_PRIORITY_REQUEST(true),
        MOVED_FOR_HIGHER_PRIORITY_REQUEST(true),
        REMOVED_FOR_HIGHER_PRIORITY_REQUEST(true),
        ADDED_AFTER_REQUEST_CANCELLATION(true),
        MOVED_AFTER_REQUEST_CANCELLATION(true),
        REMOVED_AFTER_REQUEST_CANCELLATION(true),
        ADDED_FOR_CREW_OFF_SHIFT(false),
        MOVED_FOR_CREW_OFF_SHIFT(false),
        REMOVED_FOR_CREW_OFF_SHIFT(false);

        private final boolean requestCause;

        Action(boolean requestCause) {
            this.requestCause = requestCause;
        }

        public boolean isRequestCause() {
            return requestCause;
        }

        public boolean isCrewCause() {
            return !requestCause;
        }
    }
}
