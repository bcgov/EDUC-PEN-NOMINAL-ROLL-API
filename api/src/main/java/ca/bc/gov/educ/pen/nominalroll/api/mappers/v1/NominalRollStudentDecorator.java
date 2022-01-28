package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentValidationErrorEntity;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import lombok.val;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

public abstract class NominalRollStudentDecorator implements NominalRollStudentMapper {
  private final NominalRollStudentMapper delegate;

  protected NominalRollStudentDecorator(final NominalRollStudentMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public NominalRollStudent toStruct(final NominalRollStudentEntity nominalRollStudentEntity) {
    val nomRollStudent = this.delegate.toStruct(nominalRollStudentEntity);
    nomRollStudent.setValidationErrors(nominalRollStudentEntity.getNominalRollStudentValidationErrors().stream().collect(Collectors.toMap(NominalRollStudentValidationErrorEntity::getFieldName, NominalRollStudentValidationErrorEntity::getFieldError)));
    return nomRollStudent;
  }

  @Override
  public NominalRollPostedStudentEntity toPostedEntity(final NominalRollStudentEntity nominalRollStudentEntity) {
    val postedEntity = this.delegate.toPostedEntity(nominalRollStudentEntity);
    postedEntity.setProcessingYear(LocalDateTime.now().withYear(Integer.parseInt(nominalRollStudentEntity.getProcessingYear())));
    postedEntity.setAgreementType(NominalRollHelper.getAgreementTypeMap().get(nominalRollStudentEntity.getLeaProvincial()).get(0)); // always mapped to same value.
    postedEntity.setFederalBandCode(NominalRollHelper.removeLeadingZeros(postedEntity.getFederalBandCode()));
    postedEntity.setGrade(NominalRollHelper.getGradeCodeMap().get(postedEntity.getGrade()));
    postedEntity.setBandOfResidence(NominalRollHelper.removeLeadingZeros(postedEntity.getBandOfResidence()));
    return postedEntity;
  }


}
