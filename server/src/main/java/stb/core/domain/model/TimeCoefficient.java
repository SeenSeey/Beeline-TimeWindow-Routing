package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "time_coefficients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeCoefficient extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_period_id", nullable = false)
    private TimePeriod timePeriod;

    /** ISO day of week: 1 = Monday ... 7 = Sunday */
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal coefficient;

    public TimeCoefficient(TimePeriod timePeriod, int dayOfWeek, BigDecimal coefficient) {
        this.timePeriod = timePeriod;
        this.dayOfWeek = dayOfWeek;
        this.coefficient = coefficient;
    }
}
