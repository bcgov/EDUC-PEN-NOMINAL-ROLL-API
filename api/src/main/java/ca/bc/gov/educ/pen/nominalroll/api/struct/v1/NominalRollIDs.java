package ca.bc.gov.educ.pen.nominalroll.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NominalRollIDs {
  UUID nominalRollStudentID;
}
