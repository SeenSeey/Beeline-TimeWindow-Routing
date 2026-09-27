package stb.core.domain.dto;

import stb.core.domain.model.ScheduleSlot;
import stb.core.domain.model.SlotAlterationReason;

import java.time.LocalTime;

public record SlotAlterationReasonDto(
        int id,
        SlotAlterationReason.Action action,
        String description,
        int affectedRequestId,
        int slotId,
        int slotScheduleId,
        Integer causeRequestId,
        Integer causeCrewId,
        Integer crewId,
        String crewName,
        LocalTime plannedStart,
        LocalTime plannedEnd
) {

    public static SlotAlterationReasonDto from(SlotAlterationReason reason,
                                               String description) {
        ScheduleSlot slot = reason.getSlot();
        return new SlotAlterationReasonDto(
                reason.getId(),
                reason.getAction(),
                description,
                slot.getRequest().getId(),
                slot.getId(),
                slot.getSchedule().getId(),
                reason.getCauseRequest() == null ? null : reason.getCauseRequest().getId(),
                reason.getCauseCrew() == null ? null : reason.getCauseCrew().getId(),
                slot.getEngineerCrew() == null ? null : slot.getEngineerCrew().getId(),
                slot.getEngineerCrew() == null ? null : slot.getEngineerCrew().getName(),
                slot.getStartTimeWindow(),
                slot.getEndTimeWindow()
        );
    }
}
