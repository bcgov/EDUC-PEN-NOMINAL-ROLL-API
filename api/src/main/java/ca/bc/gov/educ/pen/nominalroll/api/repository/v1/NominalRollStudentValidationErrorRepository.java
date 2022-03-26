package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentValidationErrorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NominalRollStudentValidationErrorRepository extends JpaRepository<NominalRollStudentValidationErrorEntity, UUID>, JpaSpecificationExecutor<NominalRollStudentValidationErrorEntity> {

  List<NominalRollStudentValidationErrorEntity> findAllByFieldName(String fieldName);

  @Modifying
  @Query(value = "DELETE FROM NOMINAL_ROLL_STUDENT_VALIDATION_ERROR where NOMINAL_ROLL_STUDENT_ID = :nominalRollStudentId", nativeQuery = true)
  void deleteNominalRollStudentValidationErrors(@Param("nominalRollStudentId") UUID nominalRollStudentId);
}
