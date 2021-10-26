package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentValidationError;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import lombok.val;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class NominalRollStudentDecorator implements NominalRollStudentMapper {
  private final NominalRollStudentMapper delegate;

  protected NominalRollStudentDecorator(final NominalRollStudentMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public NominalRollStudent toStruct(final NominalRollStudentEntity nominalRollStudentEntity) {
    val nomRollStudent = this.delegate.toStruct(nominalRollStudentEntity);
    nomRollStudent.setValidationErrors(nominalRollStudentEntity.getNominalRollStudentValidationErrors().stream().collect(Collectors.toMap(NominalRollStudentValidationError::getFieldName, NominalRollStudentValidationError::getFieldError)));
    return nomRollStudent;
  }

}
