package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "time_periods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimePeriod extends BaseEntity {

    @Column(name = "start_time", nullable = false, unique = true)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @OneToMany(mappedBy = "timePeriod", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeCoefficient> coefficients = new ArrayList<>();

    public TimePeriod(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public TimePeriod addCoefficient(int dayOfWeek, BigDecimal coefficient) {
        coefficients.add(new TimeCoefficient(this, dayOfWeek, coefficient));
        return this;
    }
}
