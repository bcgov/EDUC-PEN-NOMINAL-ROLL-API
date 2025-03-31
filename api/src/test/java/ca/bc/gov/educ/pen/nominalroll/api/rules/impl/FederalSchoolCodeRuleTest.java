package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class FederalSchoolCodeRuleTest {

  private static FederalSchoolCodeRule rule;


  @Mock
  NominalRollService service;


  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    rule = new FederalSchoolCodeRule(service);
  }

  @Test
  @Parameters({
    "null, 1, Field value is missing.",
    "5465, 0, null",
    "abc, 1, Field value %s is not recognized.",
    "def, 1, Field value %s is not recognized.",
    "1003, 1, Field value %s is not recognized.",
  })
  public void validate(String fedSchoolCode, int size, String fieldError) {
    if ("null".equals(fedSchoolCode)) {
      fedSchoolCode = null;
    }
    if ("null".equals(fieldError)) {
      fieldError = null;
    }
    if("5465".equals(fedSchoolCode)){
      when(this.service.getMincodeByFedBandCode("5465")).thenReturn("10200001");

     // when(restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("1002","10200001"));
    }
    val nomRoll = NominalRollStudent.builder().schoolNumber(fedSchoolCode).build();
    val result = rule.validate(NominalRollStudentMapper.mapper.toModel(nomRoll));
    assertThat(result.size()).isEqualTo(size);
    if (fieldError != null) {
      assertThat(result.get(Headers.SCHOOL_NUMBER.getCode())).isEqualTo(String.format(fieldError, fedSchoolCode));
    } else {
      assertThat(result.get(Headers.SCHOOL_NUMBER.getCode())).isNull();
    }
  }
}
