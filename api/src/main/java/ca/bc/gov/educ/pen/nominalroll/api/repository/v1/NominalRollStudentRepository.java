package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NominalRollStudentRepository extends JpaRepository<NominalRollStudentEntity, UUID>, JpaSpecificationExecutor<NominalRollStudentEntity> {

  long countByStatus(String status);

  long countAllByProcessingYear(String processingYear);

  @Query(value = "SELECT " +
    "COUNT(nominal_roll_student_id) " +
    "FROM " +
    "nominal_roll_student " +
    "WHERE " +
    "processing_year = ?1 " +
    "GROUP BY " +
    "assigned_pen " +
    "HAVING " +
    "COUNT(nominal_roll_student_id) > 1", nativeQuery = true)
  long countForDuplicateAssignedPENs(String processingYear);

  void deleteAllByProcessingYear(String processingYear);
}
