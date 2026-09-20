package stb.core.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regions")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "office_address", nullable = false)
    private String officeAddress;

    @Column(name = "office_latitude", nullable = false)
    private double officeLatitude;

    @Column(name = "office_longitude", nullable = false)
    private double officeLongitude;

    // FIXME: если важны регионы в рамках Москвы, хранение дерева зависимостей не требуется
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_region_id")
    private Region parentRegion;
}
