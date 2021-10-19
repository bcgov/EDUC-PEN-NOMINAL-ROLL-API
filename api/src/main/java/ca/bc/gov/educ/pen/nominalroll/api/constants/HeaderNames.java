package ca.bc.gov.educ.pen.nominalroll.api.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum HeaderNames {
  SCHOOL_DISTRICT_NUMBER("School District"),
  SCHOOL_NUMBER("School Number"),
  SCHOOL_NAME("School Name"),
  LEA_PROV("LEA/Provincial"),
  RECIPIENT_NUMBER("Recipient Number"),
  RECIPIENT_NAME("Recipient Name"),
  SURNAME("Surname"),
  GIVEN_NAMES("Given Name(s)"),
  INITIAL("Initial"),
  GENDER("Gender"),
  BIRTH_DATE("Birth Date"),
  GRADE("Grade"),
  FTE("FTE"),
  BAND_OF_RESIDENCE("Band of Residence");

  @Getter
  private final String code;

  HeaderNames(final String headerName) {
    this.code = headerName;
  }

  public static Optional<HeaderNames> fromString(final String headerName) {
    return Arrays.stream(HeaderNames.values()).filter(el -> headerName.equalsIgnoreCase(el.getCode())).findFirst();
  }
}
