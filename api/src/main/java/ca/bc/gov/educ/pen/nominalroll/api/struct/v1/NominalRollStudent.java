package ca.bc.gov.educ.pen.nominalroll.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;


/**
 * to convert from excel to pojo, this is a placeholder for all data records.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NominalRollStudent implements Serializable {
  private static final NominalRollStudent EMPTY = new NominalRollStudent();
  private static final long serialVersionUID = 3313662987694060598L;
  //DB PK for each row
  String nominalRollStudentID; // guid for each student.
  // fields from file
  String schoolDistrictNumber;
  String schoolNumber;
  String schoolName;
  String leaProvincial;
  String recipientNumber;
  String recipientName;
  String surname;
  String givenNames;
  String gender;
  String birthDate;
  String grade;
  String fte;
  String bandOfResidence;
  String status;

  //audit fields
  String createUser;
  String createDate;
  String updateUser;
  String updateDate;
  String processingYear;
  // fields computed.
  String assignedPEN;

  private Map<String, String> validationErrors; // key is field name and value is error for the field.

  public boolean isEmpty() {
    return this.equals(EMPTY);
  }
}
