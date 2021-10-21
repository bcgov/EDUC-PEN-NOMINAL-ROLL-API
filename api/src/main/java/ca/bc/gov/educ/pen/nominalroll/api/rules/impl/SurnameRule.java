package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.HeaderNames;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rules.BaseRule;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class SurnameRule extends BaseRule {
  @Override
  public Map<String, String> validate(final NominalRollStudentEntity nominalRollStudentEntity) {
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    if (StringUtils.isBlank(nominalRollStudentEntity.getSurname())) {
      errorsMap.put(HeaderNames.SURNAME.getCode(), "Field value is missing");
    } else if (StringUtils.length(nominalRollStudentEntity.getSurname()) > 500) {
      errorsMap.put(HeaderNames.SURNAME.getCode(), "Field value is too large");
    }
    return errorsMap;
  }
}
