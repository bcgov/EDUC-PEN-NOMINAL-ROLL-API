package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NominalRollStudentRepository extends JpaRepository<NominalRollStudent, UUID> {

  long countByStatus(String status);
}
