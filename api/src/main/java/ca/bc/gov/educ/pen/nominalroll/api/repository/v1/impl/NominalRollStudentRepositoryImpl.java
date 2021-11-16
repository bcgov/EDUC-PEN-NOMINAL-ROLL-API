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
        switch (key) {
          case ("schoolNumber"):
            sqlString.append(" AND SCHOOL_NUMBER IN (:schoolNumber)");
            break;
          case ("surname"):
            sqlString.append(" AND SURNAME LIKE :surname");
            break;
          case ("givenNames"):
            sqlString.append(" AND GIVEN_NAMES LIKE :givenNames");
            break;
          case ("gender"):
            sqlString.append(" AND GENDER = :gender");
            break;
          case ("birthDate"):
            sqlString.append(" AND BIRTH_DATE = :birthDate");
            break;
          case ("assignedPEN"):
            sqlString.append(" AND ASSIGNED_PEN = :assignedPEN");
            break;
          case ("grade"):
            sqlString.append(" AND GRADE = :grade");
            break;
          default:
            log.error("Unknown search criteria key provided for Nominal Roll Student IDs search. It is being ignored :: " + key);
            break;
        }
      });
    }

    sqlString.append(" ORDER BY SCHOOL_NUMBER ASC," +
      " SURNAME ASC," +
      " GIVEN_NAMES ASC");

    Query q = this.entityManager.createNativeQuery(sqlString.toString(), "nominalRollIDsMapping");
    q.setParameter("processingYear", processingYear);
    q.setParameter("statusCodes", statusCodes);
    if(searchCriteria != null) {
      searchCriteria.forEach(q::setParameter);
    }

    var result = q.getResultList();
    return result;
  }
}
