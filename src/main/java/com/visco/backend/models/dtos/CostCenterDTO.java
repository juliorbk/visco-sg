package com.visco.backend.models.dtos;

import com.visco.backend.models.entities.CostCenter;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CostCenterDTO {

    private Long id;
    private String code;
    private String fullDescription;
    private String divisionDescription;
    private String managementDescription;
    private String internalCc;
    private boolean active;

    public static CostCenterDTO fromEntity(CostCenter cc) {
        return CostCenterDTO.builder()
            .id(cc.getId())
            .code(cc.getCode())
            .fullDescription(cc.getFullDescription())
            .divisionDescription(cc.getDivisionDescription())
            .managementDescription(cc.getManagementDescription())
            .internalCc(cc.getInternalCc())
            .active(cc.isActive())
            .build();
    }
}
