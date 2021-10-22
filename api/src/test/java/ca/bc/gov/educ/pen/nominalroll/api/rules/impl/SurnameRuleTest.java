package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.HeaderNames;
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
public class SurnameRuleTest {

  private static SurnameRule rule;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    rule = new SurnameRule();
  }

  @Test
  @Parameters({
    "null, 1, Field value is missing.",
    "Best Surname, 0, null",
    "This is the most longest surname which is in a unit test-This is the most longest surname which is in a unit test-This is the most longest surname which is in a unit test-This is the most longest surname which is in a unit test-This is the most longest surname which is in a unit test-This is the most longest surname which is in a unit test-This is the most longest surname which is in a unit test-This is the most longest surname which is in a unit test-This is the most longest surname which is in a unit testThis is the most longest surname which is in a unit testThis is the most longest surname which is in a unit test, 1, Field value is too large.",
  })
  public void validate(String surname, int size, String fieldError) {
    if ("null".equals(surname)) {
      surname = null;
    }
    if ("null".equals(fieldError)) {
      fieldError = null;
    }
    val nomRoll = NominalRollStudent.builder().surname(surname).build();
    val result = rule.validate(NominalRollStudentMapper.mapper.toModel(nomRoll));
    assertThat(result.size()).isEqualTo(size);
    if (fieldError != null) {
      assertThat(result.get(HeaderNames.SURNAME.getCode())).isEqualTo(String.format(fieldError, surname));
    } else {
      assertThat(result.get(HeaderNames.SURNAME.getCode())).isNull();
    }
  }
}
