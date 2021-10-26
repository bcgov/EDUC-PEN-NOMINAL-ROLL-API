package ca.bc.gov.educ.pen.nominalroll.api.constants;

/**
 * The enum Event outcome.
 */
public enum EventOutcome {
  INITIATE_SUCCESS,
  /**
   * Read from topic success event outcome.
   */
  DIA_STUDENTS_CREATED,

  /**
   * Saga completed event outcome.
   */
  SAGA_COMPLETED,
  READ_FROM_TOPIC_SUCCESS,
  VALIDATION_SUCCESS_NO_ERROR,
  VALIDATION_SUCCESS_WITH_ERROR,
  PEN_MATCH_PROCESSED,
  PEN_MATCH_RESULTS_PROCESSED
  }
