package ca.bc.gov.educ.pen.nominalroll.api.rules.impl;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.helper.TestHelper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class FederalSchoolCodeRuleTest {

  private static FederalSchoolCodeRule rule;

  private static NominalRollService service;
  @Mock
  static RestUtils restUtils;

  @Autowired
  BaseNominalRollAPITest baseNominalRollAPITest;

  @Autowired
  protected TestHelper testHelper;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    rule = new FederalSchoolCodeRule(service);
  }

  @Test
  @Parameters({
    "null, 1, Field value is missing.",
    "1002, 0, null",
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
      //when(this.service.getFedProvSchoolCodes()).thenReturn(Map.of("5465","10200001"));
      val fedCodeEntity = baseNominalRollAPITest.createFedBandCode();
      testHelper.getFedProvCodeRepository().save(fedCodeEntity);
      var schoolMock = baseNominalRollAPITest.createMockSchool();
      when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
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
