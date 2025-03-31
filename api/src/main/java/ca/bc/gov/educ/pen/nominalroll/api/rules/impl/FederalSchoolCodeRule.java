package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rules.BaseRule;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The type Gender rule.
 */
@Slf4j
public class FederalSchoolCodeRule extends BaseRule {

  /**
   * The Rest utils.
   */
  private final NominalRollService service;


  public FederalSchoolCodeRule(NominalRollService service) {
    this.service = service;
  }


  @Override
  public Map<String, String> validate(final NominalRollStudentEntity nominalRollStudentEntity) {
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    val fedBandCode = nominalRollStudentEntity.getSchoolNumber();
    if (StringUtils.isBlank(fedBandCode)) {
      errorsMap.put(Headers.SCHOOL_NUMBER.getCode(), "Field value is missing.");
    } else {
      val mincode = this.service.getMincodeByFedBandCode(fedBandCode);
      if (StringUtils.isBlank(mincode)) {
        errorsMap.put(Headers.SCHOOL_NUMBER.getCode(), String.format("Field value %s is not recognized.", fedBandCode));
      }
    }
    return errorsMap;
  }
}
