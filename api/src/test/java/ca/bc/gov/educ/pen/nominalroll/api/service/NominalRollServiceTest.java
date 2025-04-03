package ca.bc.gov.educ.pen.nominalroll.api.service;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.NominalRollApiApplication;
import ca.bc.gov.educ.pen.nominalroll.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.FedProvCodeMapper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.*;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.SchoolTombstone;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NominalRollApiApplication.class)
@ActiveProfiles("test")
public class NominalRollServiceTest extends BaseNominalRollAPITest {
  private static final NominalRollStudentMapper mapper = NominalRollStudentMapper.mapper;

  @Autowired
  NominalRollStudentRepository repository;
  @Autowired
  NominalRollPostedStudentRepository postedStudentRepository;
  @Autowired
  NominalRollStudentRepositoryCustom nominalRollStudentRepositoryCustom;
  @Autowired
  NominalRollStudentValidationErrorRepository nominalRollStudentValidationErrorRepository;

  @Autowired
  FedProvCodeRepository fedProvCodeRepository;

  @Autowired
  FedProvCodeMapper fedProvCodeMapper;

  @Mock
  NominalRollService service;

  @Mock
  MessagePublisher messagePublisher;


  @Before
  public void before() {
    this.service = new NominalRollService(this.restUtils, this.messagePublisher, this.repository, this.postedStudentRepository, this.nominalRollStudentRepositoryCustom, this.nominalRollStudentValidationErrorRepository, this.fedProvCodeRepository, fedProvCodeMapper);
  }

  @AfterEach
  void cleanup(){
    fedProvCodeRepository.deleteAll();
  }
  @Test
  public void testRemoveFedProvCodes_ShouldReturnOk() {
    var schoolList = new ArrayList<SchoolTombstone>();
    schoolList.add(SchoolTombstone.builder().schoolNumber("00023").openedDate("20100101").closedDate("00000000").build());
    schoolList.add(SchoolTombstone.builder().schoolNumber("00001").openedDate("20100101").closedDate("20180101").build());
    schoolList.add(SchoolTombstone.builder().schoolNumber("00011").openedDate("20100101").closedDate(null).build());
    when(this.restUtils.getSchools()).thenReturn(schoolList);
    val fedCodeEntity = this.createFedBandCode();
    this.testHelper.getFedProvCodeRepository().save(fedCodeEntity);
    this.service.removeClosedSchoolsFedProvMappings();
    var schoolMock = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(schoolMock));
    assertNotNull(service.getFedProvSchoolCodes());
    assertNotNull(restUtils.getSchools());
    this.testHelper.getFedProvCodeRepository().delete(fedCodeEntity);
    restUtils.evictFedProvSchoolCodesCache();
  }

  @Test
  public void testRetrieveStudent_WhenStudentDoesNotExistInDB_ShouldThrowEntityNotFoundException() {
    final var studentID = UUID.fromString("00000000-0000-0000-0000-f3b2d4f20000");
    assertThrows(EntityNotFoundException.class, () -> this.service.getNominalRollStudentByID(studentID));
  }

  @Test
  public void testFindAllStudent_WhenPayloadIsValid_ShouldReturnAllStudentsObject() throws ExecutionException, InterruptedException {
    assertNotNull(this.service.findAll(null, 0, 5, new ArrayList<>()).get());
  }

  private NominalRollStudentEntity createNominalRollStudent() {
    final NominalRollStudentEntity student = new NominalRollStudentEntity();
    student.setGivenNames("John");
    student.setSurname("Wayne");
    student.setBirthDate("1907-05-26");
    student.setGender("M");
    student.setBandOfResidence("4664");
    student.setFte("1.0");
    student.setLeaProvincial("Provincial");
    student.setGrade("01");
    student.setProcessingYear("2021");
    student.setSchoolName("Test Highschool");
    student.setSchoolNumber("5465");
    student.setSchoolDistrictNumber("5");
    student.setRecipientName("Test FN Band");
    student.setRecipientNumber("8554");
    student.setStatus("LOADED");
    return student;
  }

}
