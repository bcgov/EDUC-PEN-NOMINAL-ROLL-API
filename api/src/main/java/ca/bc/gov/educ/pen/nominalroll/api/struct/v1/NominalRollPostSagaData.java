package ca.bc.gov.educ.pen.nominalroll.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Student merge complete saga data.
 */
@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NominalRollPostSagaData {
  Boolean isSavedToPosterityTable;

  public Boolean getIsSavedToPosterityTable() {
    return isSavedToPosterityTable != null && isSavedToPosterityTable;
  }
}
