package ca.bc.gov.educ.pen.nominalroll.api.service.v1;


import ca.bc.gov.educ.pen.nominalroll.api.exception.NominalRollAPIRuntimeException;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.sdc.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.District;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.DownloadableReportResponse;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.IndependentAuthority;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.SchoolTombstone;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.v1.ministryreports.NominalRollAllStudentsReportHeader.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {
    @Autowired
    private final NominalRollService nominalRollService;

    private final NominalRollStudentRepository repository;

    @Autowired
    private final RestUtils restUtils;

    public DownloadableReportResponse generateAllNominalRollStudentService(String collectionID, List<NominalRollPostedStudentEntity> postedStudents) throws JsonProcessingException {

        List<String> studentPens = postedStudents.stream()
                    .map(NominalRollPostedStudentEntity::getAssignedPEN)
                    .collect(Collectors.toList());
        long start1701 = System.currentTimeMillis();
        List<SdcSchoolCollectionStudent> sdcStudents = restUtils.getAll1701Students(collectionID, studentPens);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(SCHOOL_CATEGORY_CODE_1701.getCode(), SCHOOL_CATEGORY_CODE_NR.getCode(),
                               DISTRICT_NUMBER_1701.getCode(), DISTRICT_DISPLAY_NAME_1701.getCode(),
                               DISTRICT_NUMBER_NR.getCode(),DISTRICT_DISPLAY_NAME_NR.getCode(),
                               ASSIGNED_PEN_1701.getCode(),LEGAL_FIRST_NAME.getCode(), USUAL_FIRST_NAME.getCode(), LEGAL_MIDDLE_NAME.getCode(), USUAL_MIDDLE_NAME.getCode(),LEGAL_LAST_NAME.getCode(), USUAL_LAST_NAME.getCode(),
                               ASSIGNED_PEN_NR.getCode(), GIVEN_NAME_NR.getCode(), SURNAME_NR.getCode(),
                               DOB_1701.getCode(), DOB_NR.getCode(),
                               GENDER_1701.getCode(),GENDER_NR.getCode(),
                               GRADE_1701.getCode(),GRADE_NR.getCode(),
                               FTE_1701.getCode(),FTE_NR.getCode(),
                               AGREEMENT_NAME_NR.getCode(), SPECIAL_EDUCATION_CODE_1701.getCode(),
                               BAND_OF_RESIDENCE_CODE_1701.getCode(), RECIPIENT_BAND_CODE_NR.getCode(), RECIPIENT_BAND_NAME_NR.getCode(),
                               SCHOOL_FUNDING_CODE_1701.getCode(), INDIGENOUS_1701.getCode(),ADULT_1701.getCode(), SCHOOL_AGED.getCode(), SCHOOL_NUMBER_1701.getCode(), SCHOOL_DISPLAY_NAME_1701.getCode(), SCHOOL_FACILITY_TYPE_CODE_1701.getCode(), SCHOOL_REPORTING_REQUIREMENT_CODE_1701.getCode(),
                               SCHOOL_NUMBER_NR.getCode(), SCHOOL_DISPLAY_NAME_NR.getCode(), SCHOOL_FACILITY_TYPE_CODE_NR.getCode(), SCHOOL_REPORTING_REQUIREMENT_CODE_1701.getCode(),
                               AUTHORITY_NUMBER_1701.getCode(), AUTHORITY_DISPLAY_NAME_1701.getCode(),
                               AUTHORITY_DISPLAY_NAME_NR.getCode()
                    )
                    .build();
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
                CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

                Map<String, List<NominalRollPostedStudentEntity>> postedStudentsMap = postedStudents.stream()
                        .collect(Collectors.groupingBy(NominalRollPostedStudentEntity::getAssignedPEN));

                for (SdcSchoolCollectionStudent sdcStudent : sdcStudents) {

                    String pen = sdcStudent.getAssignedPen();
                    List<NominalRollPostedStudentEntity> matchedPostedStudents = postedStudentsMap.getOrDefault(pen, Collections.emptyList());

                    if (matchedPostedStudents.isEmpty()) {
                        // ➡ Case 1: Student not matched to any posted student — still export them
                        SchoolTombstone school = Optional.ofNullable(sdcStudent.getSchoolID())
                                .flatMap(restUtils::getSchoolBySchoolID)
                                .orElse(null);

                        District district = Optional.ofNullable(school)
                                .map(SchoolTombstone::getDistrictId)
                                .flatMap(restUtils::getDistrictByDistrictID)
                                .orElse(null);

                        IndependentAuthority authority = Optional.ofNullable(school)
                                .map(SchoolTombstone::getIndependentAuthorityId)
                                .filter(Objects::nonNull)
                                .flatMap(restUtils::getAuthorityByAuthorityID)
                                .orElse(null);

                        List<String> csvRowData = prepareAllNominalRollDataForCsv(
                                school, district, authority,
                                null, null, null,
                                sdcStudent, null
                        );
                        csvPrinter.printRecord(csvRowData);

                    } else {
                        // ➡ Case 2: Student matched to one or more posted students — export all
                        for (NominalRollPostedStudentEntity studentNR : matchedPostedStudents) {

                            String fedSchoolNumber = Optional.ofNullable(studentNR.getFederalSchoolNumber())
                                    .map(String::trim)
                                    .map(num -> String.format("%05d", Integer.parseInt(num)))
                                    .orElse(null);

                            Optional<String> mincodeOpt = Optional.ofNullable(fedSchoolNumber)
                                    .map(fed -> nominalRollService.getFedProvSchoolCodes().get(fed));

                            SchoolTombstone nrSchool = mincodeOpt
                                    .flatMap(restUtils::getSchoolByMincode)
                                    .orElse(null);

                            District nrDistrict = Optional.ofNullable(nrSchool)
                                    .map(SchoolTombstone::getDistrictId)
                                    .flatMap(restUtils::getDistrictByDistrictID)
                                    .orElse(null);

                            IndependentAuthority nrAuthority = Optional.ofNullable(nrSchool)
                                    .map(SchoolTombstone::getIndependentAuthorityId)
                                    .filter(Objects::nonNull)
                                    .flatMap(restUtils::getAuthorityByAuthorityID)
                                    .orElse(null);

                            SchoolTombstone school = Optional.ofNullable(sdcStudent.getSchoolID())
                                    .flatMap(restUtils::getSchoolBySchoolID)
                                    .orElse(null);

                            District district = Optional.ofNullable(school)
                                    .map(SchoolTombstone::getDistrictId)
                                    .flatMap(restUtils::getDistrictByDistrictID)
                                    .orElse(null);

                            IndependentAuthority authority = Optional.ofNullable(school)
                                    .map(SchoolTombstone::getIndependentAuthorityId)
                                    .filter(Objects::nonNull)
                                    .flatMap(restUtils::getAuthorityByAuthorityID)
                                    .orElse(null);

                            List<String> csvRowData = prepareAllNominalRollDataForCsv(
                                    school, district, authority,
                                    nrSchool, nrDistrict, nrAuthority,
                                    sdcStudent, studentNR
                            );
                            csvPrinter.printRecord(csvRowData);
                        }
                    }
                }
                // Collect all SDC PENs for reverse lookup
                Set<String> sdcPens = sdcStudents.stream()
                        .map(SdcSchoolCollectionStudent::getAssignedPen)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                // Now check posted students that do NOT exist in SDC list
                for (NominalRollPostedStudentEntity postedStudent : postedStudents) {
                    String pen = postedStudent.getAssignedPEN();
                    if (pen == null || sdcPens.contains(pen)) {
                        continue; // Already handled in the first loop
                    }

                    String fedSchoolNumber = Optional.ofNullable(postedStudent.getFederalSchoolNumber())
                            .map(String::trim)
                            .map(num -> String.format("%05d", Integer.parseInt(num)))
                            .orElse(null);

                    Optional<String> mincodeOpt = Optional.ofNullable(fedSchoolNumber)
                            .map(fed -> nominalRollService.getFedProvSchoolCodes().get(fed));
                    SchoolTombstone nrSchool = mincodeOpt
                            .flatMap(restUtils::getSchoolByMincode)
                            .orElse(null);

                    District nrDistrict = Optional.ofNullable(nrSchool)
                            .map(SchoolTombstone::getDistrictId)
                            .flatMap(restUtils::getDistrictByDistrictID)
                            .orElse(null);

                    IndependentAuthority nrAuthority = Optional.ofNullable(nrSchool)
                            .map(SchoolTombstone::getIndependentAuthorityId)
                            .filter(Objects::nonNull)
                            .flatMap(restUtils::getAuthorityByAuthorityID)
                            .orElse(null);

                    // Export the posted student with null SDC match
                    List<String> csvRowData = prepareAllNominalRollDataForCsv(
                            null, null, null, // No SDC school info
                            nrSchool, nrDistrict, nrAuthority,
                            null, postedStudent
                    );
                    csvPrinter.printRecord(csvRowData);
                }



                csvPrinter.flush();

                var downloadableReport = new DownloadableReportResponse();
                downloadableReport.setReportType("nominal-roll-all-students-report");
                downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

                return downloadableReport;

            } catch (IOException e) {
                throw new NominalRollAPIRuntimeException(e);
            }
    }
    private List<String> prepareAllNominalRollDataForCsv(
            SchoolTombstone school,
            District district,
            IndependentAuthority authority,
            SchoolTombstone nrSchool,
            District nrDistrict,
            IndependentAuthority nrAuthority,
            SdcSchoolCollectionStudent sdcStudent,
            NominalRollPostedStudentEntity studentNR) {

        List<String> csvRowData = new ArrayList<>();

        // Helper method to avoid repeating ternary logic
        Function<Object, String> safe = obj -> obj != null ? obj.toString() : "";

        csvRowData.addAll(Arrays.asList(

                // 1701 District
                safe.apply(school != null ? school.getSchoolCategoryCode() : null),
                safe.apply(nrSchool != null ? nrSchool.getSchoolCategoryCode() : null),

                safe.apply(district != null ? district.getDistrictNumber() : null),
                safe.apply(district != null ? district.getDisplayName() : null),
                // NR District
                safe.apply(nrDistrict != null ? nrDistrict.getDistrictNumber() : null),
                safe.apply(nrDistrict != null ? nrDistrict.getDisplayName() : null),

                // 1701 Student Info
                safe.apply(sdcStudent != null ? sdcStudent.getAssignedPen() : null),
                safe.apply(sdcStudent != null ? sdcStudent.getLegalFirstName() : null),
                safe.apply(sdcStudent != null ? sdcStudent.getUsualFirstName(): null),
                safe.apply(sdcStudent != null ? sdcStudent.getLegalMiddleNames() : null),
                safe.apply(sdcStudent != null ? sdcStudent.getUsualMiddleNames() : null),
                safe.apply(sdcStudent != null ? sdcStudent.getLegalLastName() : null),
                safe.apply(sdcStudent != null ? sdcStudent.getLegalLastName() : null),

                // NR Student Info
                safe.apply(studentNR != null ? studentNR.getAssignedPEN() : null),
                safe.apply(studentNR != null ? studentNR.getGivenNames() : null),
                safe.apply(studentNR != null ? studentNR.getSurname() : null),

                safe.apply(sdcStudent != null ? sdcStudent.getDob() : null),
                safe.apply(studentNR != null && studentNR.getBirthDate() != null ? studentNR.getBirthDate() : null),

                safe.apply(sdcStudent != null ? sdcStudent.getGender() : null),
                safe.apply(studentNR != null ? studentNR.getGender() : null),

                safe.apply(sdcStudent != null ? sdcStudent.getEnrolledGradeCode() : null),
                safe.apply(studentNR != null ? studentNR.getGrade() : null),

                safe.apply(sdcStudent != null && sdcStudent.getFte() != null ? sdcStudent.getFte() : null),
                safe.apply(studentNR != null && studentNR.getFte() != null ? studentNR.getFte() : null),

                safe.apply(studentNR != null ? studentNR.getAgreementType() : null),
                safe.apply(sdcStudent != null ? sdcStudent.getSpecialEducationCategoryCode() : null),

                safe.apply(sdcStudent != null ? sdcStudent.getBandCode() : null),
                safe.apply(studentNR != null ? studentNR.getBandOfResidence() : null),
                safe.apply(studentNR != null ? studentNR.getFederalRecipientBandName() : null),

                safe.apply(sdcStudent != null ? sdcStudent.getSchoolFundingCode() : null),
                safe.apply(sdcStudent != null ? sdcStudent.getNativeAncestryInd() : null),
                safe.apply(sdcStudent != null ? sdcStudent.getIsAdult() : null),
                safe.apply(sdcStudent != null ? sdcStudent.getIsSchoolAged() : null),
                // 1701 School
                safe.apply(school != null ? school.getSchoolNumber() : null),
                safe.apply(school != null ? school.getDisplayName() : null),

                safe.apply(school != null ? school.getFacilityTypeCode() : null),
                safe.apply(school != null ? school.getSchoolReportingRequirementCode() : null),
                // NR School
                safe.apply(nrSchool != null ? nrSchool.getSchoolNumber() : null),
                safe.apply(nrSchool != null ? nrSchool.getDisplayName() : null),

                safe.apply(nrSchool != null ? nrSchool.getFacilityTypeCode() : null),
                safe.apply(nrSchool != null ? nrSchool.getSchoolReportingRequirementCode() : null),

                // 1701 Authority
                safe.apply(authority != null ? authority.getAuthorityNumber() : null),
                safe.apply(authority != null ? authority.getDisplayName() : null),

                // NR Authority
                safe.apply(nrAuthority != null ? nrAuthority.getDisplayName() : null)
        ));

        return csvRowData;
    }


}
