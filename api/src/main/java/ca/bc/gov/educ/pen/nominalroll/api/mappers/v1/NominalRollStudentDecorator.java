package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentValidationError;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import lombok.val;

import java.util.stream.Collectors;

public abstract class NominalRollStudentDecorator implements NominalRollStudentMapper {
  private final NominalRollStudentMapper delegate;

  protected NominalRollStudentDecorator(final NominalRollStudentMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public NominalRollStudent toStruct(ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudent nominalRollStudent) {
    val nomRollStudent = this.delegate.toStruct(nominalRollStudent);
    nomRollStudent.setValidationErrors(nominalRollStudent.getNominalRollStudentValidationErrors().stream().collect(Collectors.toMap(NominalRollStudentValidationError::getFieldName, NominalRollStudentValidationError::getFieldError)));
    return nomRollStudent;
  }
}
