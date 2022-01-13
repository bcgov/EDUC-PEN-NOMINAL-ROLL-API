package ca.bc.gov.educ.pen.nominalroll.api.repository.v1.impl;

import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepositoryCustom;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollIDs;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class NominalRollStudentRepositoryImpl implements NominalRollStudentRepositoryCustom {

  @Getter(AccessLevel.PRIVATE)
  private final EntityManager entityManager;

  @Getter(AccessLevel.PRIVATE)
  private final Map<String, String> searchStatements = Map.of(
    "schoolNumber", " AND SCHOOL_NUMBER IN (:schoolNumber)",
    "surname", " AND SURNAME LIKE :surname",
    "givenNames", " AND GIVEN_NAMES LIKE :givenNames",
    "gender", " AND GENDER = :gender",
    "birthDate", " AND BIRTH_DATE = :birthDate",
    "assignedPEN", " AND ASSIGNED_PEN = :assignedPEN",
    "grade", " AND GRADE = :grade"
    );

  /**
   * Instantiates a new pen request batch student repository custom.
   *
   * @param em the entity manager
   */
  @Autowired
  NominalRollStudentRepositoryImpl(final EntityManager em) {
    this.entityManager = em;
  }


  @Override
  public List<NominalRollIDs> getAllNominalRollStudentIDs(String processingYear, List<String> statusCodes, Map<String,String> searchCriteria) {

    StringBuilder sqlString = new StringBuilder();
    sqlString.append("SELECT NOMINAL_ROLL_STUDENT_ID FROM NOMINAL_ROLL_STUDENT ");
    sqlString.append(" WHERE PROCESSING_YEAR = :processingYear AND STATUS IN (:statusCodes)");

    if(searchCriteria != null) {
      searchCriteria.forEach((key, value) -> {
        String searchString = this.getSearchStatements().get(key);
        if(searchString != null) {
          sqlString.append(searchString);
        } else {
          log.error("Unknown search criteria key provided for Nominal Roll Student IDs search. It is being ignored :: " + key);
        }
      });
    }

    sqlString.append(" ORDER BY STATUS ASC, SCHOOL_NUMBER ASC," +
      " SURNAME ASC," +
      " GIVEN_NAMES ASC");

    Query q = this.entityManager.createNativeQuery(sqlString.toString(), "nominalRollIDsMapping");
    q.setParameter("processingYear", processingYear);
    q.setParameter("statusCodes", statusCodes);
    if(searchCriteria != null) {
      searchCriteria.forEach(q::setParameter);
    }

    return q.getResultList();
  }
}
