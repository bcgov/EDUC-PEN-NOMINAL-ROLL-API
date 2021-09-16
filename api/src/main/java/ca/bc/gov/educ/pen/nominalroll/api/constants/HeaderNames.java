package ca.bc.gov.educ.pen.nominalroll.api.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum HeaderNames {
  ISC_SCHOOL_NO("ISC School Number"),
  SCHOOL_NAME("School Name"),
  SCHOOL_DISTRICT_NUMBER("School District Number"),
  SCHOOL_DISTRICT_NAME("School District Name"),
  TUITION_AGREEMENT("Tuition Agreement"),
  RECIPIENT_NUMBER("Recipient Number"),
  RECIPIENT_NAME("Recipient Name"),
  FAMILY_NAME("Family Name"),
  GIVEN_NAMES("Given Name(s)"),
  ALIAS_NAMES("Alias Name(s)"),
  DATE_OF_BIRTH("Date of Birth"),
  GENDER("Gender"),
  BAND_OF_RESIDENCE("Band of Residence"),
  GRADE_NAME("Grade Name"),
  FTE("FTE");

  @Getter
  private final String code;

  HeaderNames(final String headerName) {
    this.code = headerName;
  }

  public static Optional<HeaderNames> fromString(final String headerName) {
    return Arrays.stream(HeaderNames.values()).filter(el -> headerName.equalsIgnoreCase(el.getCode())).findFirst();
  }
}
