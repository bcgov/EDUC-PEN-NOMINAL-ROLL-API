package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GenderCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class GenderRuleTest {

  private static GenderRule rule;
  @Mock
  static RestUtils restUtils;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    rule = new GenderRule(restUtils);
  }

  @Test
  @Parameters({
    "null, 1, Field value is missing.",
    "M, 0, null",
    "F, 0, null",
    "A, 1, Gender code %s is not recognized.",
    "B, 1, Gender code %s is not recognized.",
    "C, 1, Gender code %s is not recognized.",
    "D, 1, Gender code %s is not recognized.",
    "E, 1, Gender code %s is not recognized.",
    "G, 1, Gender code %s is not recognized.",
    "H, 1, Gender code %s is not recognized.",
    "I, 1, Gender code %s is not recognized.",
    "J, 1, Gender code %s is not recognized.",
    "K, 1, Gender code %s is not recognized.",
    "L, 1, Gender code %s is not recognized.",
    "N, 1, Gender code %s is not recognized.",
    "O, 1, Gender code %s is not recognized.",
    "P, 1, Gender code %s is not recognized.",
    "Q, 1, Gender code %s is not recognized.",
    "R, 1, Gender code %s is not recognized.",
    "S, 1, Gender code %s is not recognized.",
    "T, 1, Gender code %s is not recognized.",
    "U, 1, Gender code %s is not recognized.",
    "V, 1, Gender code %s is not recognized.",
    "W, 1, Gender code %s is not recognized.",
    "X, 1, Gender code %s is not recognized.",
    "Y, 1, Gender code %s is not recognized.",
    "Z, 1, Gender code %s is not recognized.",
  })
  public void validate(String genderCode, int size, String fieldError) {
    if ("null".equals(genderCode)) {
      genderCode = null;
    }
    if ("null".equals(fieldError)) {
      fieldError = null;
    }
    if("M".equals(genderCode)){
      when(restUtils.getActiveGenderCodes()).thenReturn(List.of(GenderCode.builder().genderCode("M").build()));
    }
    if("F".equals(genderCode)){
      when(restUtils.getActiveGenderCodes()).thenReturn(List.of(GenderCode.builder().genderCode("F").build()));
    }
    val nomRoll = NominalRollStudent.builder().gender(genderCode).build();
    val result = rule.validate(NominalRollStudentMapper.mapper.toModel(nomRoll));
    assertThat(result.size()).isEqualTo(size);
    if (fieldError != null) {
      assertThat(result.get(Headers.GENDER.getCode())).isEqualTo(String.format(fieldError, genderCode));
    } else {
      assertThat(result.get(Headers.GENDER.getCode())).isNull();
    }
  }
}
