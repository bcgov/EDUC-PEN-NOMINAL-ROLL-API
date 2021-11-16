package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;

import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollIDs;

import java.util.List;
import java.util.Map;

public interface NominalRollStudentRepositoryCustom {

  /**
   * Custom query to return only ids
   *
   * @param processingYear - the processing year
   * @param statusCodes - the list of status codes
   * @param searchCriteria - the criteria used to filter requests
   * @return - the list of ids
   */
  List<NominalRollIDs> getAllNominalRollStudentIDs(String processingYear,
                                         List<String> statusCodes,
                                         Map<String, String> searchCriteria);
}
