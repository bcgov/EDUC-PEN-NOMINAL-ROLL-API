package ca.bc.gov.educ.pen.nominalroll.api.model.v1;

import ca.bc.gov.educ.pen.nominalroll.api.util.UpperCase;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * This is the posterity table which will have data populated after the post.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "NOMINAL_ROLL_POSTED_STUDENT")
@DynamicUpdate
@ToString
public class NominalRollPostedStudentEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
    @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "NOMINAL_ROLL_POSTED_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID nominalRollStudentID; //

  @Column(name = "FEDERAL_SCHOOL_BOARD", length = 3)
  String federalSchoolBoard;

  @Column(name = "FEDERAL_SCHOOL_NUMBER", length = 5)
  String federalSchoolNumber;

  @Column(name = "FEDERAL_SCHOOL_NAME", length = 255)
  @UpperCase
  String federalSchoolName;

  @Column(name = "AGREEMENT_TYPE", length = 1)
  @UpperCase
  String agreementType;

  @Column(name = "FEDERAL_BAND_CODE", length = 5)
  String federalBandCode;

  @Column(name = "FEDERAL_RECIPIENT_BAND_NAME", length = 255)
  @UpperCase
  String federalRecipientBandName;

  @Column(name = "SURNAME", length = 255)
  @UpperCase
  String surname;

  @Column(name = "GIVEN_NAMES", length = 255)
  @UpperCase
  String givenNames;

  @Column(name = "GENDER", length = 1)
  @UpperCase
  String gender;

  @Column(name = "BIRTH_DATE")
  LocalDate birthDate;

  @Column(name = "GRADE", length = 2)
  String grade;

  @Column(name = "FTE")
  BigDecimal fte;

  @Column(name = "BAND_OF_RESIDENCE", length = 10)
  String bandOfResidence;

  @Column(name = "ASSIGNED_PEN", length = 9)
  String assignedPEN;

  @Column(name = "STATUS", length = 20)
  @UpperCase
  String status;

  @Column(name = "RECORD_NUMBER", length = 5)
  Integer recordNumber;

  @Column(name = "PROCESSING_YEAR")
  LocalDateTime processingYear;

  @Column(name = "CREATE_USER", updatable = false, length = 32)
  String createUser;

  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 32)
  String updateUser;

  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;
}
