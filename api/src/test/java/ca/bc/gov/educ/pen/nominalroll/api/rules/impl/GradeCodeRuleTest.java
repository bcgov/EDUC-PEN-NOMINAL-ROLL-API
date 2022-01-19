package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.GradeCodes;
import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GradeCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class GradeCodeRuleTest {

  @Mock
  static RestUtils restUtils;
  private static GradeCodeRule rule;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    rule = new GradeCodeRule(restUtils);
  }

  @Test
  @Parameters({
    "null, 1, Field value is missing.",
    "1, 0, null",
    "01, 0, null",
    "2, 0, null",
    "02, 0, null",
    "3, 0, null",
    "03, 0, null",
    "4, 0, null",
    "04, 0, null",
    "5, 0, null",
    "05, 0, null",
    "6, 0, null",
    "06, 0, null",
    "7, 0, null",
    "07, 0, null",
    "8, 0, null",
    "08, 0, null",
    "9, 0, null",
    "09, 0, null",
    "10, 0, null",
    "11, 0, null",
    "12, 0, null",
    "K, 0, null",
    "KF, 0, null",
    "UE, 0, null",
    "EU, 0, null",
    "US, 0, null",
    "SU, 0, null",
    "123, 1, Grade code %s is not recognized.",
  })
  public void validate(String gradeCode, int size, String fieldError) {
    if ("null".equals(gradeCode)) {
      gradeCode = null;
    }
    if ("null".equals(fieldError)) {
      fieldError = null;
    }
    List<GradeCode> gradeCodes = new ArrayList<>();
    for (GradeCodes grade : GradeCodes.values()) {
      gradeCodes.add(GradeCode.builder().gradeCode(grade.getCode()).build());
    }
    when(restUtils.getActiveGradeCodes()).thenReturn(gradeCodes);
    val nomRoll = NominalRollStudent.builder().grade(gradeCode).build();
    val result = rule.validate(NominalRollStudentMapper.mapper.toModel(nomRoll));
    assertThat(result.size()).isEqualTo(size);
    if (fieldError != null) {
      assertThat(result.get(Headers.GRADE.getCode())).isEqualTo(String.format(fieldError, gradeCode));
    } else {
      assertThat(result.get(Headers.GRADE.getCode())).isNull();
    }
  }
}
