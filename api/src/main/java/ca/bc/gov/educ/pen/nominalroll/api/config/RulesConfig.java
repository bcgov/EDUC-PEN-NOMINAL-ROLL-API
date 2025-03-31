package ca.bc.gov.educ.pen.nominalroll.api.config;

import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.rules.Rule;
import ca.bc.gov.educ.pen.nominalroll.api.rules.impl.*;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class RulesConfig {

  @Bean
  @Order(1)
  public Rule surnameRule() {
    return new SurnameRule();
  }

  @Bean
  @Order(2)
  @Autowired
  public Rule genderRule(final RestUtils restUtils) {
    return new GenderRule(restUtils);
  }

  @Bean
  @Order(3)
  public Rule birthDateRule() {
    return new BirthDateRule();
  }

  @Bean
  @Order(4)
  @Autowired
  public Rule gradeCodeRule(final RestUtils restUtils) {
    return new GradeCodeRule(restUtils);
  }

  @Bean
  @Order(5)
  @Autowired
  public Rule federalSchoolCodeRule(final NominalRollService service) {
    return new FederalSchoolCodeRule(service);
  }

  @Bean
  @Order(6)
  @Autowired
  public Rule schoolDistrictRule(final RestUtils restUtils) {
    return new SchoolDistrictRule(restUtils);
  }

  @Bean
  @Order(7)
  public Rule schoolNameRule() {
    return new SchoolNameRule();
  }

  @Bean
  @Order(8)
  public Rule agreementTypeRule() {
    return new AgreementTypeRule();
  }
}
