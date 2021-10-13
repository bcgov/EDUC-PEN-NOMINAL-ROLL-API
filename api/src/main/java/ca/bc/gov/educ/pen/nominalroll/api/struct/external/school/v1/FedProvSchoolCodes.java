package ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FedProvSchoolCodes {
  String key;
  String federalCode;
  String provincialCode;
}
