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
public class BirthDateRuleTest {

  private static BirthDateRule rule;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    rule = new BirthDateRule();
  }

  @Test
  @Parameters({
    "null, 1, Field value is missing.",
    "2000-01-01, 0, null",
    "2000-02-01, 0, null",
    "2001-01-01, 0, null",
    "2002-01-01, 0, null",
    "2000-01-31, 0, null",
    "2000-11-01, 0, null",
    "2021-01-01, 0, null",
    "201-01-20, 1, Invalid BirthDate value %s \\, must be yyyy-MM-dd format.",
    "20125-01-02, 1, Invalid BirthDate value %s \\, must be yyyy-MM-dd format.",
    "2021-02-29, 1, Invalid BirthDate value %s \\, must be yyyy-MM-dd format.",
    "2020-03-32, 1, Invalid BirthDate value %s \\, must be yyyy-MM-dd format.",
  })
  public void validate(String birthDate, final int size, String fieldError) {
    if ("null".equals(birthDate)) {
      birthDate = null;
    }
    if ("null".equals(fieldError)) {
      fieldError = null;
    }
    val nomRoll = NominalRollStudent.builder().birthDate(birthDate).build();
    val result = rule.validate(NominalRollStudentMapper.mapper.toModel(nomRoll));
    assertThat(result.size()).isEqualTo(size);
    if (fieldError != null) {
      assertThat(result.get(Headers.BIRTH_DATE.getCode())).isEqualTo(String.format(fieldError, birthDate));
    } else {
      assertThat(result.get(Headers.BIRTH_DATE.getCode())).isNull();
    }

  }
}
