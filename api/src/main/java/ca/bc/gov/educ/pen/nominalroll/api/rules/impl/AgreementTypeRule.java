package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.HeaderNames;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rules.BaseRule;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgreementTypeRule extends BaseRule {
  private final List<String> agreementTypes = Arrays.asList("L", "LEA", "Local Ed. Agreement", "Local Education Agreement", "P", "Provincial", "Prov");

  @Override
  public Map<String, String> validate(final NominalRollStudentEntity nominalRollStudentEntity) {
    final Map<String, String> errorsMap = new LinkedHashMap<>();
    if (StringUtils.isBlank(nominalRollStudentEntity.getLeaProvincial())) {
      errorsMap.put(HeaderNames.LEA_PROV.getCode(), "Field value is missing");
    } else if (!this.agreementTypes.contains(nominalRollStudentEntity.getLeaProvincial())) {
      errorsMap.put(HeaderNames.LEA_PROV.getCode(), String.format("Invalid LEA/Provincial value %s", nominalRollStudentEntity.getLeaProvincial()));
    }
    return errorsMap;
  }
}
