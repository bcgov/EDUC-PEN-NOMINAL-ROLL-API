package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.UUIDMapper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@DecoratedWith(NominalRollStudentDecorator.class)
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface NominalRollStudentMapper {
  NominalRollStudentMapper mapper = Mappers.getMapper(NominalRollStudentMapper.class);

  @Mapping(target = "validationErrors", ignore = true)
  NominalRollStudent toStruct(NominalRollStudentEntity nominalRollStudentEntity);

  @Mapping(target = "status", constant = "LOADED")
  @Mapping(target = "nominalRollStudentValidationErrors", ignore = true)
  NominalRollStudentEntity toModel(NominalRollStudent nominalRollStudent);

  NominalRollStudent toStructure(NominalRollStudentEntity studentEntity);
}
