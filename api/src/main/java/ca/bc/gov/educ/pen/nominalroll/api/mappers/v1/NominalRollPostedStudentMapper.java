package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.UUIDMapper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.sld.v1.SldDiaStudent;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@DecoratedWith(NominalRollPostedStudentDecorator.class)
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface NominalRollPostedStudentMapper {
  NominalRollPostedStudentMapper mapper = Mappers.getMapper(NominalRollPostedStudentMapper.class);

  @Mapping(target = "recordNumber", ignore = true)
  @Mapping(target = "federalSchoolNumber", source = "schoolNumber")
  @Mapping(target = "federalSchoolName", source = "schoolName")
  @Mapping(target = "federalSchoolBoard", source = "schoolDistrictNumber")
  @Mapping(target = "federalRecipientBandName", source = "recipientName")
  @Mapping(target = "federalBandCode", source = "recipientNumber")
  @Mapping(target = "agreementType", source = "leaProvincial")
  NominalRollPostedStudentEntity toPostedStudentEntity(NominalRollStudentEntity nominalRollStudentEntity);

  @Mapping(target = "withdrawalCode", ignore = true)
  @Mapping(target = "usualSurname", ignore = true)
  @Mapping(target = "usualGiven", ignore = true)
  @Mapping(target = "studSurname", ignore = true)
  @Mapping(target = "studSex", ignore = true)
  @Mapping(target = "studNewFlag", ignore = true)
  @Mapping(target = "studMiddle", ignore = true)
  @Mapping(target = "studGrade", ignore = true)
  @Mapping(target = "studGiven", ignore = true)
  @Mapping(target = "studBirth", ignore = true)
  @Mapping(target = "sitenoNew", ignore = true)
  @Mapping(target = "siteno", ignore = true)
  @Mapping(target = "schtype", ignore = true)
  @Mapping(target = "schoolName", ignore = true)
  @Mapping(target = "schnum", ignore = true)
  @Mapping(target = "schlnoNew", ignore = true)
  @Mapping(target = "schlNo", ignore = true)
  @Mapping(target = "schboard", ignore = true)
  @Mapping(target = "reportDate", ignore = true)
  @Mapping(target = "postedPen", ignore = true)
  @Mapping(target = "penStatus", ignore = true)
  @Mapping(target = "penComment", ignore = true)
  @Mapping(target = "pen", source = "assignedPEN")
  @Mapping(target = "origPen", ignore = true)
  @Mapping(target = "fteVal", ignore = true)
  @Mapping(target = "frbandnum", ignore = true)
  @Mapping(target = "distnoNew", ignore = true)
  @Mapping(target = "distNo", ignore = true)
  @Mapping(target = "diaSchoolInfoWrong", ignore = true)
  @Mapping(target = "comment", ignore = true)
  @Mapping(target = "bandresnum", ignore = true)
  @Mapping(target = "bandname", ignore = true)
  @Mapping(target = "bandCode", ignore = true)
  SldDiaStudent toDiaStudent(NominalRollPostedStudentEntity nominalRollPostedStudentEntity);
}
