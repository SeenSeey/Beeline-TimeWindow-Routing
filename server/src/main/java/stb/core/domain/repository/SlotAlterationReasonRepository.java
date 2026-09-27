package stb.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import stb.core.domain.model.SlotAlterationReason;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

@Repository
public interface SlotAlterationReasonRepository extends JpaRepository<SlotAlterationReason, Integer> {

    default List<SlotAlterationReason> findAllForCurrentTransition(int currentScheduleId,
                                                                   int previousScheduleId) {
        EnumSet<SlotAlterationReason.Action> currentSlotActions = EnumSet.of(
                SlotAlterationReason.Action.ADDED_FOR_HIGHER_PRIORITY_REQUEST,
                SlotAlterationReason.Action.MOVED_FOR_HIGHER_PRIORITY_REQUEST,
                SlotAlterationReason.Action.ADDED_AFTER_REQUEST_CANCELLATION,
                SlotAlterationReason.Action.MOVED_AFTER_REQUEST_CANCELLATION,
                SlotAlterationReason.Action.ADDED_FOR_CREW_OFF_SHIFT,
                SlotAlterationReason.Action.MOVED_FOR_CREW_OFF_SHIFT);
        EnumSet<SlotAlterationReason.Action> removedActions = EnumSet.of(
                SlotAlterationReason.Action.REMOVED_FOR_HIGHER_PRIORITY_REQUEST,
                SlotAlterationReason.Action.REMOVED_AFTER_REQUEST_CANCELLATION,
                SlotAlterationReason.Action.REMOVED_FOR_CREW_OFF_SHIFT);
        return findAllForCurrentTransitionByActions(
                currentScheduleId, previousScheduleId, currentSlotActions, removedActions);
    }

    @Query("""
            SELECT reason FROM SlotAlterationReason reason
            JOIN FETCH reason.slot slot
            JOIN FETCH slot.schedule schedule
            JOIN FETCH slot.request
            LEFT JOIN FETCH slot.engineerCrew
            LEFT JOIN FETCH reason.causeRequest
            LEFT JOIN FETCH reason.causeCrew
            WHERE (schedule.id = :currentScheduleId AND reason.action IN :currentSlotActions)
               OR (schedule.id = :previousScheduleId AND reason.action IN :removedActions)
            ORDER BY reason.id
            """)
    List<SlotAlterationReason> findAllForCurrentTransitionByActions(
            int currentScheduleId,
            int previousScheduleId,
            Collection<SlotAlterationReason.Action> currentSlotActions,
            Collection<SlotAlterationReason.Action> removedActions);
}
