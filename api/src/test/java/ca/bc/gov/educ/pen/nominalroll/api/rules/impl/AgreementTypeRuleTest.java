package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class AgreementTypeRuleTest {
  private static AgreementTypeRule rule;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    rule = new AgreementTypeRule();
  }


  @Test
  @Parameters({
    "null, 1, Field value is missing.",
    "L, 0, null",
    "LEA, 0, null",
    "Local Ed. Agreement, 0, null",
    "Local Education Agreement, 0, null",
    "P, 0, null",
    "Provincial, 0, null",
    "Prov, 0, null",
    "blah blah, 1, Invalid LEA/Provincial value %s",
    "junk, 1, Invalid LEA/Provincial value %s",
    "ah, 1, Invalid LEA/Provincial value %s",
    "abc, 1, Invalid LEA/Provincial value %s",
  })
  public void validate(String agreementType, final int size, String fieldError) {
    if ("null".equals(agreementType)) {
      agreementType = null;
    }
    if ("null".equals(fieldError)) {
      fieldError = null;
    }
    val nomRoll = NominalRollStudent.builder().leaProvincial(agreementType).build();
    val result = rule.validate(NominalRollStudentMapper.mapper.toModel(nomRoll));
    assertThat(result.size()).isEqualTo(size);
    if(fieldError != null){
      assertThat(result.get(Headers.LEA_PROV.getCode())).isEqualTo(String.format(fieldError, agreementType));
    }else {
      assertThat(result.get(Headers.LEA_PROV.getCode())).isNull();
    }

  }
}
