ALTER TABLE NOMINAL_ROLL_SAGA
    MODIFY (
        PAYLOAD BLOB
        );

ALTER TABLE NOMINAL_ROLL_SAGA_EVENT_STATES
    MODIFY (
        SAGA_EVENT_RESPONSE BLOB
        );
