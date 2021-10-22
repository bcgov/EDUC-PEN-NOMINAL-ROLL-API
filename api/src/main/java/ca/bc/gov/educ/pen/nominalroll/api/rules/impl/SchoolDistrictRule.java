package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.HeaderNames;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.rules.BaseRule;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class SchoolDistrictRule extends BaseRule {
  private final RestUtils restUtils;

  public SchoolDistrictRule(final RestUtils restUtils) {
    this.restUtils = restUtils;
  }

  @Override
  public Map<String, String> validate(final NominalRollStudentEntity nominalRollStudentEntity) {
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    if (StringUtils.isBlank(nominalRollStudentEntity.getSchoolDistrictNumber())) {
      errorsMap.put(HeaderNames.SCHOOL_DISTRICT_NUMBER.getCode(), "Field value is missing.");
    } else {
      val schoolDistNumber = StringUtils.leftPad(nominalRollStudentEntity.getSchoolDistrictNumber(), 3, "0");
      if (!this.restUtils.districtCodes().contains(schoolDistNumber)) {
        errorsMap.put(HeaderNames.SCHOOL_DISTRICT_NUMBER.getCode(), String.format("School District Number %s is not recognized.", nominalRollStudentEntity.getSchoolDistrictNumber()));
      }
    }
    return errorsMap;
  }
}
