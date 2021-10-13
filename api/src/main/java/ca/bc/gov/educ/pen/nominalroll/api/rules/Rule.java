package ca.bc.gov.educ.pen.nominalroll.api.rules;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudent;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface Rule {

  /**
   * This method will be implemented by each child class for specific rule.
   * @param nominalRollStudent the object to be validated.
   * @return the List of Errors Map, the map
   */
  List<Map<String, String>> validate(NominalRollStudent nominalRollStudent);
}
