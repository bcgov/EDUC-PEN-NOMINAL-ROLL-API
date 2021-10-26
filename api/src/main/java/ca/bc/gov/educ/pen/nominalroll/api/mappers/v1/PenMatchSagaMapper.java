package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.UUIDMapper;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.penmatch.v1.PenMatchStudent;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen match saga mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface PenMatchSagaMapper {
  /**
   * The constant mapper.
   */
  PenMatchSagaMapper mapper = Mappers.getMapper(PenMatchSagaMapper.class);

  @Mapping(target = "usualSurname", ignore = true)
  @Mapping(target = "usualMiddleName", ignore = true)
  @Mapping(target = "usualGivenName", ignore = true)
  @Mapping(target = "postal", ignore = true)
  @Mapping(target = "pen", ignore = true)
  @Mapping(target = "middleName", ignore = true)
  @Mapping(target = "localID", ignore = true)
  @Mapping(target = "sex", source = "nominalRollStudent.gender")
  @Mapping(target = "givenName", source = "nominalRollStudent.givenNames")
  @Mapping(target = "enrolledGradeCode", source = "nominalRollStudent.grade")
  @Mapping(target = "dob", source = "nominalRollStudent.birthDate")
  @Mapping(target = "surname", source = "nominalRollStudent.surname")
  PenMatchStudent toPenMatchStudent(NominalRollStudent nominalRollStudent, String mincode);
}
