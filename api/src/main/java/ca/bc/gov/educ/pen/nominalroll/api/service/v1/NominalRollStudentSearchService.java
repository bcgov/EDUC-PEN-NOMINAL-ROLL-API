package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

import ca.bc.gov.educ.pen.nominalroll.api.filter.NominalRollStudentFilterSpecs;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import org.springframework.stereotype.Service;

/**
 * The type Student search service.
 */
@Service
public class NominalRollStudentSearchService extends SearchService<NominalRollStudentEntity>{

  /**
   * Instantiates a new Student search service.
   *
   * @param nominalRollStudentFilterSpecs the student filter specs
   */
  public NominalRollStudentSearchService(NominalRollStudentFilterSpecs nominalRollStudentFilterSpecs) {
    super(nominalRollStudentFilterSpecs, NominalRollStudentEntity.class);
  }
}
