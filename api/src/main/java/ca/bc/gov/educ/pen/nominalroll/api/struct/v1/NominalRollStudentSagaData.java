package ca.bc.gov.educ.pen.nominalroll.api.struct.v1;

import ca.bc.gov.educ.pen.nominalroll.api.struct.external.penmatch.v1.PenMatchResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NominalRollStudentSagaData implements Serializable {
  private static final long serialVersionUID = -2329245910142215178L;
  private NominalRollStudent nominalRollStudent;
  private PenMatchResult penMatchResult;
}
