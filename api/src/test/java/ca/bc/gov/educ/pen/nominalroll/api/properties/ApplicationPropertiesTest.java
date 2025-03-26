package ca.bc.gov.educ.pen.nominalroll.api.properties;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationPropertiesTest extends BaseNominalRollAPITest {

  @Autowired
  ApplicationProperties applicationProperties;

  @Test
  public void testPropertyPlaceholder_givenProfile_shouldHaveValidValues() {
    assertThat(this.applicationProperties.getFolderBasePath()).isEqualTo("/home/runner/work/EDUC-PEN-NOMINAL-ROLL-API/EDUC-PEN-NOMINAL-ROLL-API/temp");
    assertThat(this.applicationProperties.getClientID()).isEqualTo("123");
    assertThat(this.applicationProperties.getClientSecret()).isEqualTo("123");
    assertThat(this.applicationProperties.getStudentApiURL()).isEqualTo("https://asdf.com");
    assertThat(this.applicationProperties.getTokenURL()).isEqualTo("https://abcxyz.com");
    assertThat(this.applicationProperties.getIsHttpRampUp()).isFalse();
    assertThat(this.applicationProperties.getConnectionName()).isEqualTo("NOMINAL-ROLL-API");
    assertThat(this.applicationProperties.getMaxReconnect()).isEqualTo(60);
    assertThat(this.applicationProperties.getNominalRollInvalidFieldThreshold()).isEqualTo(100);
  }


}
