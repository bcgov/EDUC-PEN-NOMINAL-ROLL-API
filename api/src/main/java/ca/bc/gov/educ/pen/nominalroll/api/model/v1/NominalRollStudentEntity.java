package ca.bc.gov.educ.pen.nominalroll.api.model.v1;

import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollIDs;
import ca.bc.gov.educ.pen.nominalroll.api.util.UpperCase;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
@SqlResultSetMapping(name="nominalRollIDsMapping", classes = {
  @ConstructorResult(targetClass = NominalRollIDs.class,
    columns = {@ColumnResult(name="NOMINAL_ROLL_STUDENT_ID", type = UUID.class)})
})
@DynamicUpdate
@ToString
public class NominalRollStudentEntity {

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
  @UpperCase
  String schoolName;

  @Column(name = "LEA_PROVINCIAL", length = 500)
  @UpperCase
  String leaProvincial;

  @Column(name = "RECIPIENT_NUMBER", length = 500)
  String recipientNumber;

  @Column(name = "RECIPIENT_NAME", length = 500)
  @UpperCase
  String recipientName;

  @Column(name = "SURNAME", length = 500)
  @UpperCase
  String surname;

  @Column(name = "GIVEN_NAMES", length = 500)
  @UpperCase
  String givenNames;

  @Column(name = "GENDER", length = 500)
  @UpperCase
  String gender;

  @Column(name = "BIRTH_DATE", length = 500)
  String birthDate;

  @Column(name = "GRADE", length = 500)
  String grade;

  @Column(name = "FTE", length = 500)
  String fte;

  @Column(name = "BAND_OF_RESIDENCE", length = 500)
  String bandOfResidence;

  @Column(name = "ASSIGNED_PEN", length = 9)
  String assignedPEN;

  @Column(name = "STATUS", length = 20)
  @UpperCase
  String status;

  @Column(name = "PROCESSING_YEAR", length = 4)
  String processingYear;

  @Column(name = "CREATE_USER", updatable = false, length = 32)
  String createUser;

  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 32)
  String updateUser;

  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;

  /**
   * The Pen request batch student entities.
   */
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @Setter(AccessLevel.NONE) // no setter, only getter as it will affect the attached entity.
  @OneToMany(mappedBy = "nominalRollStudent", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = NominalRollStudentValidationError.class)
  Set<NominalRollStudentValidationError> nominalRollStudentValidationErrors;

  public Set<NominalRollStudentValidationError> getNominalRollStudentValidationErrors() {
    if (this.nominalRollStudentValidationErrors == null) {
      this.nominalRollStudentValidationErrors = new HashSet<>();
    }
    return nominalRollStudentValidationErrors;
  }
}
