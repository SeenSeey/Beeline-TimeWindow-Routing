package stb.core.application.request;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.domain.dto.GeoPosition;
import stb.core.domain.dto.RequestCreateDto;
import stb.core.domain.exception.DomainException;
import stb.core.domain.model.JobType;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;
import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.model.RequestStatus;
import stb.core.domain.port.Geocoder;
import stb.core.domain.repository.EquipmentRepository;
import stb.core.domain.repository.JobTypeRepository;
import stb.core.domain.repository.RegionRepository;
import stb.core.domain.repository.RequestRepository;
import stb.core.domain.repository.RequestStatusRepository;
import stb.core.domain.repository.ScheduleRepository;
import stb.core.domain.repository.ScheduleSlotRepository;
import stb.core.domain.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RequestWriteAuditUseCasesTest {

    @Test
    void createsRequestAndCreationAudit() {
        RequestRepository requests = mock(RequestRepository.class);
        RequestStatusRepository statuses = mock(RequestStatusRepository.class);
        JobTypeRepository jobTypes = mock(JobTypeRepository.class);
        RegionRepository regions = mock(RegionRepository.class);
        Geocoder geocoder = mock(Geocoder.class);
        RequestActionAuditWriter audits = mock(RequestActionAuditWriter.class);
        when(statuses.findAll()).thenReturn(List.of(new RequestStatus("Новая", RequestStatus.Keyword.IDLE)));
        when(geocoder.convertAddressToCoordinates("Адрес")).thenReturn(new GeoPosition("Адрес", 55.7, 37.6));
        when(jobTypes.findById(1)).thenReturn(Optional.of(
                new JobType("Монтаж", List.of(), 20, 60, 90, JobType.Keyword.INSTALLATION, 3)));
        when(regions.findById(2)).thenReturn(Optional.of(new Region("Регион", "Офис", 55.7, 37.6, null)));

        new RequestCreateUseCase(requests, statuses, mock(VehicleRepository.class), mock(EquipmentRepository.class),
                jobTypes, regions, geocoder, mock(ApplicationEventPublisher.class), mock(ScheduleRepository.class), audits)
                .execute(new RequestCreateDto(2, "Адрес", 1, null, null, null, null));

        ArgumentCaptor<Request> saved = ArgumentCaptor.forClass(Request.class);
        verify(requests).save(saved.capture());
        assertSavedAction(audits, RequestActionAudit.Action.REQUEST_CREATED, saved.getValue());
    }

    @Test
    void auditsStartCompleteAndCancelAfterSuccessfulMutation() {
        RequestStatus assigned = new RequestStatus("Assigned", RequestStatus.Keyword.ASSIGNED);
        RequestStatus inProgress = new RequestStatus("In progress", RequestStatus.Keyword.IN_PROGRESS);
        RequestStatus completed = new RequestStatus("Completed", RequestStatus.Keyword.COMPLETED);
        RequestStatus canceled = new RequestStatus("Canceled", RequestStatus.Keyword.CANCELED);

        Request startRequest = Request.builder().status(assigned).build();
        RequestActionAuditWriter startAudits = writerFor(startRequest, List.of(inProgress),
                (requests, statuses, audits) -> new RequestMarkInProgressUseCase(requests, statuses, audits).execute(1));
        assertSavedAction(startAudits, RequestActionAudit.Action.STARTED, startRequest);

        Request completeRequest = Request.builder().status(inProgress).arrivedAt(LocalDateTime.now()).build();
        RequestActionAuditWriter completeAudits = writerFor(completeRequest, List.of(completed),
                (requests, statuses, audits) -> new RequestCompleteUseCase(requests, statuses, audits).execute(1));
        assertSavedAction(completeAudits, RequestActionAudit.Action.COMPLETED, completeRequest);

        Request cancelRequest = Request.builder().status(assigned).build();
        RequestRepository cancelRequests = mock(RequestRepository.class);
        RequestStatusRepository cancelStatuses = mock(RequestStatusRepository.class);
        RequestActionAuditWriter cancelAudits = mock(RequestActionAuditWriter.class);
        when(cancelRequests.findById(1)).thenReturn(Optional.of(cancelRequest));
        when(cancelStatuses.findAll()).thenReturn(List.of(canceled));
        new RequestCancelUseCase(cancelRequests, cancelStatuses, mock(ApplicationEventPublisher.class),
                mock(ScheduleSlotRepository.class), mock(ScheduleRepository.class), cancelAudits)
                .execute(1);
        assertSavedAction(cancelAudits, RequestActionAudit.Action.CANCELED, cancelRequest);
    }

    @Test
    void failedDomainValidationDoesNotWriteAudit() {
        Request request = Request.builder()
                .status(new RequestStatus("Assigned", RequestStatus.Keyword.ASSIGNED))
                .build();
        RequestRepository requests = mock(RequestRepository.class);
        RequestStatusRepository statuses = mock(RequestStatusRepository.class);
        RequestActionAuditWriter audits = mock(RequestActionAuditWriter.class);
        when(requests.findById(1)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> new RequestCompleteUseCase(requests, statuses, audits).execute(1))
                .isInstanceOf(DomainException.ConflictException.class);

        verifyNoInteractions(audits);
    }

    private static RequestActionAuditWriter writerFor(Request request,
                                                      List<RequestStatus> statusValues,
                                                      UseCaseInvocation invocation) {
        RequestRepository requests = mock(RequestRepository.class);
        RequestStatusRepository statuses = mock(RequestStatusRepository.class);
        RequestActionAuditWriter audits = mock(RequestActionAuditWriter.class);
        when(requests.findById(1)).thenReturn(Optional.of(request));
        when(statuses.findAll()).thenReturn(statusValues);
        invocation.execute(requests, statuses, audits);
        return audits;
    }

    private static void assertSavedAction(RequestActionAuditWriter writer,
                                          RequestActionAudit.Action action,
                                          Request request) {
        ArgumentCaptor<RequestActionAudit> captor = ArgumentCaptor.forClass(RequestActionAudit.class);
        verify(writer).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(action);
        assertThat(captor.getValue().getRequest()).isSameAs(request);
        assertThat(captor.getValue().getTimestamp()).isNotNull();
    }

    private interface UseCaseInvocation {
        void execute(RequestRepository requests,
                     RequestStatusRepository statuses,
                     RequestActionAuditWriter audits);
    }
}
