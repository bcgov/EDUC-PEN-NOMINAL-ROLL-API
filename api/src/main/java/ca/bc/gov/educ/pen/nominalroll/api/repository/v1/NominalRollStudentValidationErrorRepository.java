package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentValidationErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NominalRollStudentValidationErrorRepository extends JpaRepository<NominalRollStudentValidationErrorEntity, UUID>, JpaSpecificationExecutor<NominalRollStudentValidationErrorEntity> {

  List<NominalRollStudentValidationErrorEntity> findAllByFieldName(String fieldName);
  void deleteAllByNominalRollStudent(NominalRollStudentEntity nominalRollStudent);
}
