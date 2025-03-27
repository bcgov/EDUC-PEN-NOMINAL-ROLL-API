package ca.bc.gov.educ.pen.nominalroll.api.model.v1;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The type FedProvCode entity.
 *
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "FED_BAND_CODE")
@DynamicUpdate
@ToString
public class FedProvCodeEntity {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
          @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "FED_BAND_CODE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID fedBandCodeID;

  @Basic
  @Column(name = "SCHOOL_ID", columnDefinition = "BINARY(16)", updatable = false)
  private UUID schoolID;

  @Basic
  @Column(name = "FED_BAND_CODE")
  private String fedBandCode;

  @Column(name = "CREATE_USER", updatable = false, length = 32)
  String createUser;

  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 32)
  String updateUser;

  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;
}
