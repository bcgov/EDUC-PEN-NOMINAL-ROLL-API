package ca.bc.gov.educ.pen.nominalroll.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;

/**
 * The type Nominal roll post saga data.
 */
@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NominalRollPostSagaData extends BaseRequest {
  @NotNull( message = "processingYear cannot be null")
  String processingYear;
}
