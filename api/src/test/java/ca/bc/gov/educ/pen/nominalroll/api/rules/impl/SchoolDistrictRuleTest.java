package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
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
public class SchoolDistrictRuleTest {

  @Mock
  static RestUtils restUtils;
  private static SchoolDistrictRule rule;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    rule = new SchoolDistrictRule(restUtils);
  }

  @Test
  @Parameters({
    "null, 1, Field value is missing.",
    "6, 0, null",
    "06, 0, null",
    "006, 0, null",
    "102, 0, null",
    "103, 0, null",
    "21, 0, null",
    "021, 0, null",
    "008, 1,School District Number %s is not recognized.",
    "08, 1,School District Number %s is not recognized.",
    "8, 1,School District Number %s is not recognized.",
    "009, 1,School District Number %s is not recognized.",
    "09, 1,School District Number %s is not recognized.",
    "9, 1,School District Number %s is not recognized.",
    "007, 1,School District Number %s is not recognized.",
    "07, 1,School District Number %s is not recognized.",
    "7, 1,School District Number %s is not recognized.",
    "005, 1,School District Number %s is not recognized.",
    "05, 1,School District Number %s is not recognized.",
    "5, 1,School District Number %s is not recognized.",
    "004, 1,School District Number %s is not recognized.",
    "04, 1,School District Number %s is not recognized.",
    "4, 1,School District Number %s is not recognized.",
    "003, 1,School District Number %s is not recognized.",
    "03, 1,School District Number %s is not recognized.",
    "3, 1,School District Number %s is not recognized.",

  })
  public void validate(String schoolDistrict, int size, String fieldError) {
    if ("null".equals(schoolDistrict)) {
      schoolDistrict = null;
    }
    if ("null".equals(fieldError)) {
      fieldError = null;
    }
    when(restUtils.districtCodes()).thenReturn(List.of("102", "103", "021", "006"));
    val nomRoll = NominalRollStudent.builder().schoolDistrictNumber(schoolDistrict).build();
    val result = rule.validate(NominalRollStudentMapper.mapper.toModel(nomRoll));
    assertThat(result.size()).isEqualTo(size);
    if (fieldError != null) {
      assertThat(result.get(Headers.SCHOOL_DISTRICT_NUMBER.getCode())).isEqualTo(String.format(fieldError, schoolDistrict));
    } else {
      assertThat(result.get(Headers.SCHOOL_DISTRICT_NUMBER.getCode())).isNull();
    }
  }
}
