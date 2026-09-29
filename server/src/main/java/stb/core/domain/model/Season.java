package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seasons")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Season extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal coefficient;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeasonMonth> months = new ArrayList<>();

    public Season(String name, BigDecimal coefficient, int... months) {
        this.name = name;
        this.coefficient = coefficient;

        for (int month : months) {
            this.months.add(new SeasonMonth(month, this));
        }
    }
}