package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;


import ca.bc.gov.educ.pen.nominalroll.api.model.v1.FedProvCodeEntity;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.FedProvSchoolCode;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * The interface FedProvCode mapper.
 */
@Mapper(componentModel = "spring")
@DecoratedWith(FedProvCodeDecorator.class)
public interface FedProvCodeMapper {

  /**
   * The constant mapper.
   */

  List<FedProvSchoolCode> toStructList(List<FedProvCodeEntity> fedProvCodeEntities);

  FedProvSchoolCode toStruct(FedProvCodeEntity fedProvCodeEntity);


}
