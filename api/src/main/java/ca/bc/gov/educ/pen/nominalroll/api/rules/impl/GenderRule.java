package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.rules.BaseRule;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GenderCode;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenderRule extends BaseRule {

  private final RestUtils restUtils;

  public GenderRule(final RestUtils restUtils) {
    this.restUtils = restUtils;
  }

  @Override
  public Map<String, String> validate(final NominalRollStudentEntity nominalRollStudentEntity) {
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    final List<String> genders = this.restUtils.getActiveGenderCodes().stream().map(GenderCode::getGenderCode).collect(Collectors.toList());
    if (StringUtils.isBlank(nominalRollStudentEntity.getGender())) {
      errorsMap.put(Headers.GENDER.getCode(), "Field value is missing.");
    } else if (!genders.contains(nominalRollStudentEntity.getGender().toUpperCase())) {
      errorsMap.put(Headers.GENDER.getCode(), String.format("Gender code %s is not recognized.", nominalRollStudentEntity.getGender()));
    }
    return errorsMap;
  }
}
