package ca.bc.gov.educ.pen.nominalroll.api.model.v1;

import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * This is the initial load table which will have data populated from the file itself. this table needs to hold junk data. this is the reason everything is varchar and 500 characters length.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "NOMINAL_ROLL_STUDENT")
@DynamicUpdate
@ToString
public class NominalRollStudent {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
    @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "NOMINAL_ROLL_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID nominalRollStudentID; //

  @Column(name = "SCHOOL_DISTRICT_NUMBER", length = 500)
  String schoolDistrictNumber;

  @Column(name = "SCHOOL_NUMBER", length = 500)
  String schoolNumber;

  @Column(name = "SCHOOL_NAME", length = 500)
  String schoolName;

  @Column(name = "LEA_PROVINICIAL", length = 500)
  String leaProvincial;

  @Column(name = "RECIPIENT_NUMBER", length = 500)
  String recipientNumber;

  @Column(name = "RECIPIENT_NAME", length = 500)
  String recipientName;

  @Column(name = "IDENTITY", length = 500)
  String identity;

  @Column(name = "SURNAME", length = 500)
  String surnmae;

  @Column(name = "GIVEN_NAMES", length = 500)
  String givenNames;

  @Column(name = "INITIAL", length = 500)
  String initial;

  @Column(name = "GENDER", length = 500)
  String gender;

  @Column(name = "BIRTH_DATE", length = 500)
  String birthDate;


  @Column(name = "GRADE", length = 500)
  String grade;

  @Column(name = "FTE", length = 500)
  String fte;

  @Column(name = "BAND_OF_RESIDENCE", length = 500)
  String bandOfResidence;

  @Column(name = "YEAR_OF_PROCESSING", length = 4)
  String yearOfProcessing;

  @Column(name = "ASSIGNED_PEN", length = 9)
  String assignedPEN;

  @Column(name = "CREATE_USER", updatable = false, length = 32)
  String createUser;

  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 32)
  String updateUser;

  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;
}
