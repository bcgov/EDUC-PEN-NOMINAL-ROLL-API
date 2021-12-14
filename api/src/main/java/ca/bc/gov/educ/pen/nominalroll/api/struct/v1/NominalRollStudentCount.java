package ca.bc.gov.educ.pen.nominalroll.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * The type Nominal roll student count.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class NominalRollStudentCount {
  String status;
  long count;
}
