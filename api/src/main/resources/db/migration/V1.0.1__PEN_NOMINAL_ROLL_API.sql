CREATE TABLE NOMINAL_ROLL_POSTED_STUDENT
(
    NOMINAL_ROLL_POSTED_STUDENT_ID RAW(16)              NOT NULL,
    FEDERAL_SCHOOL_BOARD           NUMBER(3)            NOT NULL,
    FEDERAL_SCHOOL_NUMBER          NUMBER(5)            NOT NULL,
    FEDERAL_SCHOOL_NAME            VARCHAR2(255)        NOT NULL,
    AGREEMENT_TYPE                 VARCHAR2(1)          NOT NULL,
    FEDERAL_BAND_CODE              NUMBER(5)            NOT NULL,
    FEDERAL_RECIPIENT_BAND_NAME    VARCHAR2(255)        NOT NULL,
    SURNAME                        VARCHAR2(255)        NOT NULL,
    GIVEN_NAMES                    VARCHAR2(255),
    GENDER                         VARCHAR2(1)          NOT NULL,
    BIRTH_DATE                     DATE                 NOT NULL,
    GRADE                          VARCHAR2(2)          NOT NULL,
    FTE                            NUMBER(2,1)          NOT NULL,
    BAND_OF_RESIDENCE              NUMBER(10)           NOT NULL,
    ASSIGNED_PEN                   VARCHAR2(9),
    STATUS                         VARCHAR2(20)         NOT NULL,
    RECORD_NUMBER                  NUMBER(5)            NOT NULL,
    PROCESSING_YEAR                DATE                 NOT NULL,
    CREATE_USER                    VARCHAR2(32)         NOT NULL,
    CREATE_DATE                    DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER                    VARCHAR2(32)         NOT NULL,
    UPDATE_DATE                    DATE DEFAULT SYSDATE NOT NULL,

    CONSTRAINT NOMINAL_ROLL_POSTED_STUDENT_PK PRIMARY KEY (NOMINAL_ROLL_POSTED_STUDENT_ID)
) TABLESPACE API_PEN_DATA;

ALTER TABLE NOMINAL_ROLL_STUDENT
    ADD PROCESSING_YEAR  VARCHAR2(4) NOT NULL;

ALTER TABLE NOMINAL_ROLL_STUDENT
    DROP COLUMN IDENTITY;
