package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.HeaderNames;
import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rules.BaseRule;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class BirthDateRule extends BaseRule {
  @Override
  public Map<String, String> validate(final NominalRollStudentEntity nominalRollStudentEntity) {
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    if (StringUtils.isBlank(nominalRollStudentEntity.getBirthDate())) {
      errorsMap.put(HeaderNames.BIRTH_DATE.getCode(), "Field value is missing.");
    } else if (!NominalRollHelper.isValidDate(nominalRollStudentEntity.getBirthDate())) {
      errorsMap.put(HeaderNames.BIRTH_DATE.getCode(), String.format("Invalid BirthDate value %s , must be yyyy-MM-dd format.", nominalRollStudentEntity.getBirthDate()));
    }
    return errorsMap;
  }
}
