package stb.core.domain.dto;

import stb.core.domain.model.EngineerCrewEquipment;
import stb.core.domain.model.Equipment;

import java.util.List;
import java.util.Optional;

public record EquipmentDto(int id, String displayName, int quantity) {

    public static EquipmentDto of(Equipment equipment, int quantity) {
        return new EquipmentDto(equipment.getId(), equipment.getDisplayName(), quantity);
    }

    public static List<EquipmentDto> listFrom(List<EngineerCrewEquipment> equipment) {
        return Optional.ofNullable(equipment).orElse(List.of()).stream()
                .filter(item -> item.getQuantity() > 0)
                .map(item -> of(item.getEquipment(), item.getQuantity()))
                .toList();
    }
}
