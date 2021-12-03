package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import lombok.val;

public abstract class NominalRollPostedStudentDecorator implements NominalRollPostedStudentMapper {
  private final NominalRollPostedStudentMapper delegate;

  protected NominalRollPostedStudentDecorator(final NominalRollPostedStudentMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public NominalRollPostedStudentEntity toPostedStudentEntity(final NominalRollStudentEntity nominalRollStudentEntity) {
    val entity = this.delegate.toPostedStudentEntity(nominalRollStudentEntity);
    entity.setAgreementType(NominalRollHelper.getAgreementTypeMap().get(entity.getAgreementType()).get(0));
    return entity;
  }
}
