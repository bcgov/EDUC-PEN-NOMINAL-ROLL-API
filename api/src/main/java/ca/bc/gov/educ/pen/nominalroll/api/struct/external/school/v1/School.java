package ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * The type Student.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class School implements Serializable {
  private static final long serialVersionUID = 3035573903652774243L;

  @Size(max = 3)
  @NotNull(message = "distNo can not be null.")
  private String distNo;

  @Size(max = 5)
  @NotNull(message = "schlNo can not be null.")
  private String schlNo;

  @Size(max = 8)
  private String openedDate;

  @Size(max = 8)
  private String closedDate;
}
