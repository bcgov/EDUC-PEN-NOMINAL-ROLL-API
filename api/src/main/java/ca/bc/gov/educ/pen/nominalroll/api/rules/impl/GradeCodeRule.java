package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.rules.BaseRule;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GradeCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The type Gender rule.
 */
@Slf4j
public class GradeCodeRule extends BaseRule {

  /**
   * The Rest utils.
   */
  private final RestUtils restUtils;

  /**
   * Instantiates a new Gender rule.
   *
   * @param restUtils the rest utils
   */
  public GradeCodeRule(final RestUtils restUtils) {
    this.restUtils = restUtils;
  }


  @Override
  public Map<String, String> validate(final NominalRollStudentEntity nominalRollStudentEntity) {
    final List<String> gradeCodes = this.restUtils.getActiveGradeCodes().stream().map(GradeCode::getGradeCode).collect(Collectors.toList());
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    if (!StringUtils.isBlank(nominalRollStudentEntity.getGrade()) && !gradeCodes.contains(NominalRollHelper.getGradeCodeMap().get(nominalRollStudentEntity.getGrade().toUpperCase()))) {
      errorsMap.put(Headers.GRADE.getCode(), String.format("Grade code %s is not recognized.", nominalRollStudentEntity.getGrade()));
    }
    return errorsMap;
  }
}
