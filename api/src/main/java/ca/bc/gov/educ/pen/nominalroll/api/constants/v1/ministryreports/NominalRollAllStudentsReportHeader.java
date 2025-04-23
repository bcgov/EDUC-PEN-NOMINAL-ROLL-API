package ca.bc.gov.educ.pen.nominalroll.api.constants.v1.ministryreports;

import lombok.Getter;

@Getter
public enum NominalRollAllStudentsReportHeader {


    SCHOOL_NUMBER_1701("1701 School Number"),
    SCHOOL_DISPLAY_NAME_1701("1701 School Display Name"),
    SCHOOL_ORGANIZATION_CODE_1701("1701 School Organization Code"),
    SCHOOL_CATEGORY_CODE_1701("1701 School Category Code"),
    SCHOOL_FACILITY_TYPE_CODE_1701("1701 Facility Type Code"),
    SCHOOL_REPORTING_REQUIREMENT_CODE_1701("1701 School Reporting Requirement code"),

    DISTRICT_NUMBER_1701("1701 District Number"),
    DISTRICT_DISPLAY_NAME_1701("1701 District Display Name"),
    DISTRICT_REGION_CODE_1701("1701 District Region Code"),
    DISTRICT_STATUS_CODE_1701("1701 District Status Code"),

    AUTHORITY_NUMBER_1701("1701 Authority Number"),

    AUTHORITY_DISPLAY_NAME_1701("1701 Authority Display Name"),


    SCHOOL_NUMBER_NR("NR School Number"),

    SCHOOL_DISPLAY_NAME_NR("NR School Display Name"),
    SCHOOL_ORGANIZATION_CODE_NR("NR School Organization Code"),
    SCHOOL_CATEGORY_CODE_NR("NR School Category Code"),
    SCHOOL_FACILITY_TYPE_CODE_NR("NR Facility Type Code"),
    SCHOOL_REPORTING_REQUIREMENT_CODE_NR("NR School Reporting Requirement code"),

    DISTRICT_NUMBER_NR("NR District Number"),
    DISTRICT_DISPLAY_NAME_NR("NR District Display Name"),
    DISTRICT_REGION_CODE_NR("NR District Region Code"),
    DISTRICT_STATUS_CODE_NR("NR District Status Code"),

    AUTHORITY_NUMBER_NR("NR Authority Number"),
    AUTHORITY_DISPLAY_NAME_NR("NR Authority Display Name"),

    LEGAL_FIRST_NAME("1701 Legal First Name"),
    LEGAL_MIDDLE_NAME("1701 Legal Middle Name"),
    LEGAL_LAST_NAME("1701 Legal Last Name"),

    USUAL_FIRST_NAME("1701 USUAL First Name"),
    USUAL_MIDDLE_NAME("1701 USUAL Middle Name"),
    USUAL_LAST_NAME("1701 USUAL Last Name"),

    ASSIGNED_PEN_1701("1701 Assigned PEN"),
    DOB_1701("1701 DOB"),
    GENDER_1701("1701 Gender"),
    FTE_1701("1701 FTE"),
    SCHOOL_FUNDING_CODE_1701("1701 School Funding Code"),
    ADULT_1701("1701 Adult"),
    SCHOOL_AGED("1701 School Aged"),
    SPECIAL_EDUCATION_CODE_1701("1701 Special Need Code"),
    INDIGENOUS_1701("1701_INDIGENOUS"),

    GRADE_1701("1701 Grade"),

    ASSIGNED_PEN_NR("NR Assigned PEN"),
    GIVEN_NAME_NR("NR Given Name"),
    SURNAME_NR("NR Surname"),
    DOB_NR("NR DOB"),
    GENDER_NR("NR Gender"),
    FTE_NR("NR FTE"),
    GRADE_NR("NR Grade"),
    AGREEMENT_NAME_NR("NR Agreement Name"),

    BAND_OF_RESIDENCE_CODE_1701("1701 Band of Residence Code"),
    RECIPIENT_BAND_CODE_NR("NR Recipient Band Code"),
    RECIPIENT_BAND_NAME_NR("NR Recipient Band Name");
    private final String code;
    NominalRollAllStudentsReportHeader(String code) { this.code = code; }
}
