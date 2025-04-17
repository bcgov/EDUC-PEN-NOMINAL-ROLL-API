package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.AssignedPenEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NominalRollPostedStudentRepository extends JpaRepository<NominalRollPostedStudentEntity, UUID> {
  List<NominalRollPostedStudentEntity> findAllBySurnameAndGivenNamesAndBirthDateAndGenderOrderByCreateDateDesc(String surname, String givenNames, LocalDate birthDate, String gender);

  List<NominalRollPostedStudentEntity> findAllByProcessingYearBetween(LocalDateTime startTime, LocalDateTime endTime);
  boolean existsByProcessingYearBetween(LocalDateTime startTime, LocalDateTime endTime);

  @Query("SELECT new ca.bc.gov.educ.pen.nominalroll.api.model.v1.AssignedPenEntity(nrps.assignedPEN) " +
          "FROM NominalRollPostedStudentEntity nrps " +
          "WHERE nrps.processingYear BETWEEN :startTime AND :endTime")
  List<AssignedPenEntity>findAssignedPensByProcessingYearBetween(LocalDateTime startTime, LocalDateTime endTime);
}
