package stb.core.application.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
// import stb.core.application.support.audit.RequestActionAuditWriter;
import stb.core.domain.dto.GeoPosition;
import stb.core.domain.dto.RequestCreateDto;
import stb.core.domain.dto.RequestDto;
import stb.core.domain.event.EmergencyCreatedEvent;
import stb.core.domain.exception.*;
import stb.core.domain.model.JobType;
import stb.core.domain.model.Region;
import stb.core.domain.model.Request;
// import stb.core.domain.model.RequestActionAudit;
import stb.core.domain.model.Schedule;
import stb.core.domain.port.Geocoder;
import stb.core.domain.repository.*;

import java.time.LocalDate;
// import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestCreateUseCase {

    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final VehicleRepository vehicleRepository;
    private final EquipmentRepository equipmentRepository;
    private final JobTypeRepository jobTypeRepository;
    private final RegionRepository regionRepository;
    private final Geocoder geocoder;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ScheduleRepository scheduleRepository;
    // private final RequestActionAuditWriter auditWriter;

    @Transactional
    public RequestDto execute(RequestCreateDto createDto) {
        if (createDto.address() == null) {
            throw new DomainException.ValidationException("Необходимо указать адрес");
        }

        GeoPosition coordinates = geocoder.convertAddressToCoordinates(createDto.address());

        if (coordinates == null) {
            throw new DomainException.ValidationException("Не удалось найти координаты для адреса: %s".formatted(createDto.address()));
        }

        JobType jobType = jobTypeRepository.findById(createDto.jobTypeId())
                .orElseThrow(() -> new JobTypeNotFoundException("Тип работ с ID = %s не найден".formatted(createDto.jobTypeId())));

        Region region = regionRepository.findById(createDto.regionId())
                .orElseThrow(() -> new RegionNotFoundException("Регион с ID = %s не найден".formatted(createDto.regionId())));

        Request request = new Request(
                createDto.address(),
                coordinates.latitude(),
                coordinates.longitude(),
                requestStatusRepository.findAll(),
                jobType,
                region,
                createDto.startTimeWindow(),
                createDto.endTimeWindow()
        );

        if (createDto.requiredVehicleId() != null) {
            request.addVehicle(vehicleRepository.findById(createDto.requiredVehicleId())
                    .orElseThrow(() -> new VehicleNotFoundException("Транспорт %s не найден".formatted(createDto.requiredVehicleId()))));
        }

        if (createDto.requiredEquipmentId() != null) {
            request.addEquipment(equipmentRepository.findById(createDto.requiredEquipmentId())
                    .orElseThrow(() -> new EquipmentNotFoundException("Оборудование %s не найдено".formatted(createDto.requiredEquipmentId()))));
        }

        requestRepository.save(request);
        // auditWriter.save(RequestActionAudit.requestCreated(request, LocalDateTime.now()));

        if (request.isUrgent()) {
            LocalDate targetDate = LocalDate.now();

            Schedule schedule = scheduleRepository.findByTargetDateAndRegionId(targetDate, region.getId())
                            .orElse(null);

            if (schedule != null) {
                schedule.markAsRecalculating();
                scheduleRepository.save(schedule);

                applicationEventPublisher.publishEvent(new EmergencyCreatedEvent(request.getId(), region.getId()));
            } else {
                log.info("Расписание для {} не найдено", targetDate);
            }
        }

        return RequestDto.toDto(request);
    }
}
