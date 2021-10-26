package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NominalRollPostedStudentRepository extends JpaRepository<NominalRollPostedStudentEntity, UUID> {
  List<NominalRollPostedStudentEntity> findAllBySurnameAndGivenNamesAndBirthDateAndGenderAndGradeOrderByCreateDateDesc(String surname, String givenNames, LocalDateTime birthDate, String gender, String grade);
}