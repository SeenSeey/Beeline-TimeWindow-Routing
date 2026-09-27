package stb.core.domain.repository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import stb.core.domain.model.SlotAlterationReason;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlotAlterationReasonRepositoryTest {

    @Test
    void currentTransitionUsesCurrentSlotsForAddedAndMovedAndPreviousSlotsForRemoved() {
        SlotAlterationReasonRepository repository = mock(
                SlotAlterationReasonRepository.class, CALLS_REAL_METHODS);
        when(repository.findAllForCurrentTransitionByActions(
                eq(11), eq(10), anyCollection(), anyCollection())).thenReturn(List.of());

        repository.findAllForCurrentTransition(11, 10);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<SlotAlterationReason.Action>> currentActions =
                ArgumentCaptor.forClass(Collection.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<SlotAlterationReason.Action>> removedActions =
                ArgumentCaptor.forClass(Collection.class);
        verify(repository).findAllForCurrentTransitionByActions(
                eq(11), eq(10), currentActions.capture(), removedActions.capture());
        assertThat(currentActions.getValue()).containsExactlyInAnyOrder(
                SlotAlterationReason.Action.ADDED_FOR_HIGHER_PRIORITY_REQUEST,
                SlotAlterationReason.Action.MOVED_FOR_HIGHER_PRIORITY_REQUEST,
                SlotAlterationReason.Action.ADDED_AFTER_REQUEST_CANCELLATION,
                SlotAlterationReason.Action.MOVED_AFTER_REQUEST_CANCELLATION,
                SlotAlterationReason.Action.ADDED_FOR_CREW_OFF_SHIFT,
                SlotAlterationReason.Action.MOVED_FOR_CREW_OFF_SHIFT);
        assertThat(removedActions.getValue()).containsExactlyInAnyOrder(
                SlotAlterationReason.Action.REMOVED_FOR_HIGHER_PRIORITY_REQUEST,
                SlotAlterationReason.Action.REMOVED_AFTER_REQUEST_CANCELLATION,
                SlotAlterationReason.Action.REMOVED_FOR_CREW_OFF_SHIFT);
    }
}
