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
@Table(name = "NOMINAL_ROLL_STUDENT_VALIDATION_ERROR")
@DynamicUpdate
@ToString
public class NominalRollStudentValidationError {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
    @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "NOMINAL_ROLL_STUDENT_VALIDATION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID nominalRollStudentValidationErrorID;
  /**
   * The Pen request batch entity.
   */
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = NominalRollStudentEntity.class)
  @JoinColumn(name = "NOMINAL_ROLL_STUDENT_ID", referencedColumnName = "NOMINAL_ROLL_STUDENT_ID", updatable = false)
  NominalRollStudentEntity nominalRollStudent;

  @Column(name = "FIELD_NAME", length = 50)
  String fieldName;

  @Column(name = "FIELD_ERROR", length = 200)
  String fieldError;

  @Column(name = "CREATE_USER", updatable = false, length = 32)
  String createUser;

  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 32)
  String updateUser;

  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;
}
