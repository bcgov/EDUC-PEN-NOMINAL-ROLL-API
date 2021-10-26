package ca.bc.gov.educ.pen.nominalroll.api.constants;

/**
 * The enum Event type.
 */
public enum EventType {

  /**
   * Initiated event type.
   */
  INITIATED,

  /**
   * Get student event type.
   */
  CREATE_DIA_STUDENTS,

  /**
   * Create student event type.
   */
  CREATE_POSTED_STUDENTS,

  /**
   * Mark saga complete event type.
   */
  MARK_SAGA_COMPLETE,
  READ_FROM_TOPIC,
  VALIDATE_NOMINAL_ROLL_STUDENT,
  PROCESS_PEN_MATCH,
  PROCESS_PEN_MATCH_RESULTS
}
