package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rules.BaseRule;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgreementTypeRule extends BaseRule {

  @Override
  public Map<String, String> validate(final NominalRollStudentEntity nominalRollStudentEntity) {
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    if (StringUtils.isBlank(nominalRollStudentEntity.getLeaProvincial())) {
      errorsMap.put(Headers.LEA_PROV.getCode(), "Field value is missing.");
    } else if (NominalRollHelper.getAgreementTypeMap().keySet().stream().noneMatch(k -> k.equalsIgnoreCase(nominalRollStudentEntity.getLeaProvincial()))) {
      errorsMap.put(Headers.LEA_PROV.getCode(), String.format("Invalid LEA/Provincial value %s", nominalRollStudentEntity.getLeaProvincial()));
    }
    return errorsMap;
  }
}
