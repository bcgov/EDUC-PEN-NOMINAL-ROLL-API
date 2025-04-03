package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.FedProvCodeEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.FedProvSchoolCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public abstract class FedProvCodeDecorator implements FedProvCodeMapper {

    @Autowired
    private RestUtils restUtils;

    @Autowired
    private FedProvCodeMapper delegate; // Inject the original MapStruct mapper

    @Override
    public List<FedProvSchoolCode> toStructList(List<FedProvCodeEntity> entities) {
        return entities.stream().map(entity -> {
            FedProvSchoolCode dto = delegate.toStruct(entity);

            // Fetch and set provincialCode
            if (entity.getSchoolID() != null) {
                dto.setKey("NOM_SCHL");
                dto.setFederalCode(entity.getFedBandCode());
                dto.setProvincialCode(
                        restUtils.getSchoolBySchoolID(entity.getSchoolID().toString()).get().getMincode()

                );
            } else {
                dto.setProvincialCode("UNKNOWN");
            }

            return dto;
        }).collect(Collectors.toList());
    }
}