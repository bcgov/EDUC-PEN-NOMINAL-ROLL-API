package ca.bc.gov.educ.pen.nominalroll.api.constants.v1;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum NominalRollStudentStatus {
  LOADED("LOADED"),
  ERROR("ERROR"),
  MATCHEDSYS("MATCHEDSYS"),
  FIXABLE("FIXABLE"),
  MATCHEDUSR("MATCHEDUSR"),
  IGNORED("IGNORED");

  /**
   * The constant codeMap.
   */
  private static final Map<String, NominalRollStudentStatus> codeMap = new HashMap<>();

  static {
    for (NominalRollStudentStatus status : values()) {
      codeMap.put(status.getCode(), status);
    }
  }

  /**
   * The Code.
   */
  private final String code;

  /**
   * Instantiates a new nominal roll student status.
   *
   * @param code the code
   */
  NominalRollStudentStatus(String code) {
    this.code = code;
  }

  /**
   * Value of code nominal roll student status codes.
   *
   * @param code the code
   * @return the nominal roll student status
   */
  public static NominalRollStudentStatus valueOfCode(String code) {
    return codeMap.get(code);
  }

  /**
   * To string string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return this.getCode();
  }
}
