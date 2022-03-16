package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.mappers.LocalDateMapper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.UUIDMapper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.sld.v1.SldDiaStudent;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@DecoratedWith(NominalRollPostedStudentDecorator.class)
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, LocalDateMapper.class})
public interface NominalRollPostedStudentMapper {
  NominalRollPostedStudentMapper mapper = Mappers.getMapper(NominalRollPostedStudentMapper.class);

  @Mapping(target = "schboard", ignore = true)
  @Mapping(target = "schnum", source = "nominalRollPostedStudentEntity.federalSchoolNumber")
  @Mapping(target = "schoolName", ignore = true)
  @Mapping(target = "agreementType", source = "nominalRollPostedStudentEntity.agreementType")
  @Mapping(target = "schtype", ignore = true)
  @Mapping(target = "frbandnum", source = "nominalRollPostedStudentEntity.federalBandCode")
  @Mapping(target = "bandCode", source = "nominalRollPostedStudentEntity.federalBandCode")
  @Mapping(target = "bandname", ignore = true)
  @Mapping(target = "studSurname", ignore = true)
  @Mapping(target = "studGiven", ignore = true)
  @Mapping(target = "studMiddle", ignore = true)
  @Mapping(target = "studSex", source = "nominalRollPostedStudentEntity.gender")
  @Mapping(target = "studBirth", ignore = true)
  @Mapping(target = "studGrade", source = "nominalRollPostedStudentEntity.grade")
  @Mapping(target = "fteVal", ignore = true)
  @Mapping(target = "bandresnum", source = "nominalRollPostedStudentEntity.bandOfResidence")
  @Mapping(target = "reportDate", ignore = true)
  @Mapping(target = "recordNumber", source = "nominalRollPostedStudentEntity.recordNumber")
  @Mapping(target = "pen", source = "nominalRollPostedStudentEntity.assignedPEN")
  @Mapping(target = "postedPen", source = "nominalRollPostedStudentEntity.assignedPEN")
  @Mapping(target = "usualSurname", ignore = true)
  @Mapping(target = "usualGiven", ignore = true)
  @Mapping(target = "distNo", ignore = true)
  @Mapping(target = "schlNo", ignore = true)
  @Mapping(target = "comment", ignore = true)
  @Mapping(target = "penStatus", ignore = true)
  @Mapping(target = "origPen", ignore = true)
  @Mapping(target = "siteno", ignore = true)
  @Mapping(target = "withdrawalCode", ignore = true)
  @Mapping(target = "diaSchoolInfoWrong", ignore = true)
  @Mapping(target = "distnoNew", ignore = true)
  @Mapping(target = "schlnoNew", ignore = true)
  @Mapping(target = "sitenoNew", ignore = true)
  @Mapping(target = "studNewFlag", ignore = true)
  @Mapping(target = "penComment", ignore = true)
  SldDiaStudent toDiaStudent(NominalRollPostedStudentEntity nominalRollPostedStudentEntity, RestUtils restUtils);

}
