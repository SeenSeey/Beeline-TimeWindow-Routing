package stb.core.domain.event;

import java.time.LocalDate;

public record SchedulesCreatedEvent(LocalDate targetDate) {}
