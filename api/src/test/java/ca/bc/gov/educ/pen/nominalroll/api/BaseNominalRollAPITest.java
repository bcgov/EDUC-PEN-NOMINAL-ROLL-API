package ca.bc.gov.educ.pen.nominalroll.api;

import ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum;
import ca.bc.gov.educ.pen.nominalroll.api.helper.TestHelper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.FedProvCodeEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudentSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.INITIATED;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum.IN_PROGRESS;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.SchoolTombstone;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = NominalRollApiApplication.class)
@AutoConfigureMockMvc
public abstract class BaseNominalRollAPITest {

  @MockBean
  protected RestUtils restUtils;

  @Autowired
  protected TestHelper testHelper;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    Mockito.reset(this.restUtils);
  }

  /**
   * need to delete the records to make it work in unit tests assertion, else the records will keep growing and assertions will fail.
   */
  @After
  public void after() {
    this.testHelper.cleanDB();
  }

  @SneakyThrows
  protected Saga creatMockSaga(final NominalRollStudent student) {
    return Saga.builder()
      .sagaId(UUID.randomUUID())
      .updateDate(LocalDateTime.now().minusMinutes(15))
      .createUser(ApplicationProperties.API_NAME)
      .updateUser(ApplicationProperties.API_NAME)
      .createDate(LocalDateTime.now().minusMinutes(15))
      .sagaName(SagaEnum.NOMINAL_ROLL_PROCESS_STUDENT_SAGA.toString())
      .status(IN_PROGRESS.toString())
      .sagaState(INITIATED.toString())
      .payload(JsonUtil.getJsonStringFromObject(NominalRollStudentSagaData.builder().nominalRollStudent(student == null ? this.createMockNominalRollStudent() : student).build()))
      .build();
  }

  public SchoolTombstone createMockSchool() {
    final SchoolTombstone schoolTombstone = new SchoolTombstone();
    schoolTombstone.setSchoolId(UUID.randomUUID().toString());
    schoolTombstone.setDistrictId(UUID.randomUUID().toString());
    schoolTombstone.setDisplayName("Harry's school");
    schoolTombstone.setMincode("10200001");
    schoolTombstone.setSchoolNumber("36018");
    schoolTombstone.setOpenedDate("1964-09-01T00:00:00");
    schoolTombstone.setSchoolCategoryCode("PUBLIC");
    schoolTombstone.setSchoolReportingRequirementCode("REGULAR");
    schoolTombstone.setFacilityTypeCode("STANDARD");
    return schoolTombstone;
  }
  protected FedProvCodeEntity createFedBandCode() {
    FedProvCodeEntity fedProvCodeEntity = new FedProvCodeEntity();
    fedProvCodeEntity.setFedBandCode("5465");
    fedProvCodeEntity.setFedBandCodeID(UUID.randomUUID());
    fedProvCodeEntity.setSchoolID(UUID.randomUUID());
    fedProvCodeEntity.setUpdateUser(ApplicationProperties.API_NAME);
    fedProvCodeEntity.setCreateUser(ApplicationProperties.API_NAME);
    fedProvCodeEntity.setCreateDate(LocalDateTime.now());
    return fedProvCodeEntity;
  }
  protected NominalRollStudent createMockNominalRollStudent() {
    final NominalRollStudent student = new NominalRollStudent();
    student.setGivenNames("John");
    student.setSurname("Wayne");
    student.setBirthDate("1907-05-26");
    student.setGender("M");
    student.setBandOfResidence("4664");
    student.setFte("1.0");
    student.setLeaProvincial("Provincial");
    student.setGrade("01");
    student.setProcessingYear(String.valueOf(LocalDate.now().getYear()));
    student.setSchoolName("Test Highschool");
    student.setSchoolNumber("5465");
    student.setSchoolDistrictNumber("5");
    student.setRecipientName("Test FN Band");
    student.setRecipientNumber("8554");
    student.setStatus("LOADED");
    return student;
  }
}
