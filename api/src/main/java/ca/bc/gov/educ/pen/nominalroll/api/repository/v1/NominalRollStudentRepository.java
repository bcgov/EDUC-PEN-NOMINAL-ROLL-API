package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NominalRollStudentRepository extends JpaRepository<NominalRollStudent, UUID> {

  long countByStatus(String status);

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

}
