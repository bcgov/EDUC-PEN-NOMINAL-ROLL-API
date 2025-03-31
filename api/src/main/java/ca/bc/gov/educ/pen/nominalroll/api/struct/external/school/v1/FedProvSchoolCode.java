package ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1;

import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.BaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FedProvSchoolCode extends BaseRequest {
  String key;
  String federalCode;
  String provincialCode;
}
