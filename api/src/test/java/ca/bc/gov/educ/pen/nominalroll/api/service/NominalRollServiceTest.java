package ca.bc.gov.educ.pen.nominalroll.api.service;

import ca.bc.gov.educ.pen.nominalroll.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollPostedStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepositoryCustom;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentValidationErrorRepository;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.School;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@DataJpaTest
public class NominalRollServiceTest {
  private static final NominalRollStudentMapper mapper = NominalRollStudentMapper.mapper;

  @Autowired
  NominalRollStudentRepository repository;
  @Autowired
  NominalRollPostedStudentRepository postedStudentRepository;
  @Autowired
  NominalRollStudentRepositoryCustom nominalRollStudentRepositoryCustom;
  @Autowired
  NominalRollStudentValidationErrorRepository nominalRollStudentValidationErrorRepository;

  @Mock
  RestUtils restUtils;

  NominalRollService service;

  @Mock
  MessagePublisher messagePublisher;

  @Before
  public void before() {
    this.service = new NominalRollService(this.restUtils, this.messagePublisher, this.repository, this.postedStudentRepository, this.nominalRollStudentRepositoryCustom, this.nominalRollStudentValidationErrorRepository);
  }

  @Test
  public void testRemoveFedProvCodes_ShouldReturnOk() {
    when(this.restUtils.getSchools()).thenReturn(List.of(School.builder().distNo("504").schlNo("00001").openedDate("20100101").closedDate("20180101").build()));
    when(this.restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("5465", "50400001"));
    this.service.removeClosedSchoolsFedProvMappings();
    verify(this.restUtils, atMost(1)).getFedProvSchoolCodes();
    verify(this.restUtils, atMost(1)).getSchools();
    verify(this.restUtils, atMost(1)).deleteFedProvCode(any());
  }

  @Test
  public void testRemoveFedProvCodesMultipleSchools_ShouldReturnOk() {
    when(this.restUtils.getSchools()).thenReturn(List.of(School.builder().distNo("504").schlNo("00001").openedDate("20100101").closedDate("00000000").build()));
    when(this.restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("5465", "50400001"));
    this.service.removeClosedSchoolsFedProvMappings();
    verify(this.restUtils, atMost(1)).getFedProvSchoolCodes();
    verify(this.restUtils, atMost(1)).getSchools();
    verify(this.restUtils, atMost(0)).deleteFedProvCode(any());
  }

  @Test
  public void testRemoveFedProvCodesMultipleSchoolsNullCloseDate_ShouldReturnOk() {
    when(this.restUtils.getSchools()).thenReturn(List.of(School.builder().distNo("504").schlNo("00001").openedDate("20100101").closedDate(null).build()));
    when(this.restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("5465", "50400001"));
    this.service.removeClosedSchoolsFedProvMappings();
    verify(this.restUtils, atMost(1)).getFedProvSchoolCodes();
    verify(this.restUtils, atMost(1)).getSchools();
    verify(this.restUtils, atMost(0)).deleteFedProvCode(any());
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
