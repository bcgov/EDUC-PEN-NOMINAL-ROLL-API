package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.CacheNames;
import ca.bc.gov.educ.pen.nominalroll.api.constants.HeaderNames;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.rules.BaseRule;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.CacheService;
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
  private final RestUtils restUtils;


  public FederalSchoolCodeRule(final RestUtils restUtils) {
    this.restUtils = restUtils;
  }


  @Override
  public Map<String, String> validate(final NominalRollStudentEntity nominalRollStudentEntity) {
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    val schoolNum = nominalRollStudentEntity.getSchoolNumber();
    if (StringUtils.isBlank(schoolNum)) {
      errorsMap.put(HeaderNames.SCHOOL_NUMBER.getCode(), "Field value is missing.");
    } else {
      val mincode = this.restUtils.getFedProvSchoolCodes().get(schoolNum);
      if (StringUtils.isBlank(mincode)) {
        errorsMap.put(HeaderNames.SCHOOL_NUMBER.getCode(), String.format("Field value %s is not recognized.", schoolNum));
      }
    }
    return errorsMap;
  }
}
