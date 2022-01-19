package ca.bc.gov.educ.pen.nominalroll.api.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum Headers {
  SCHOOL_DISTRICT_NUMBER("School District", ColumnType.STRING),
  SCHOOL_NUMBER("School Number", ColumnType.STRING),
  SCHOOL_NAME("School Name", ColumnType.STRING),
  LEA_PROV("LEA/Provincial", ColumnType.STRING),
  RECIPIENT_NUMBER("Recipient Number", ColumnType.STRING),
  RECIPIENT_NAME("Recipient Name", ColumnType.STRING),
  SURNAME("Surname", ColumnType.STRING),
  GIVEN_NAMES("Given Name(s)", ColumnType.STRING),
  GENDER("Gender", ColumnType.STRING),
  BIRTH_DATE("Birth Date", ColumnType.DATE),
  GRADE("Grade", ColumnType.STRING),
  FTE("FTE", ColumnType.DATE),
  BAND_OF_RESIDENCE("Band of Residence", ColumnType.STRING);

  @Getter
  private final String code;
  @Getter
  private final ColumnType type;

  Headers(final String headerName, final ColumnType headerType) {
    this.code = headerName;
    this.type = headerType;
  }

  public static Optional<Headers> fromString(final String headerName) {
    return Arrays.stream(Headers.values()).filter(el -> headerName.equalsIgnoreCase(el.getCode())).findFirst();
  }
}
