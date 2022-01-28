package ca.bc.gov.educ.pen.nominalroll.api.service;

import ca.bc.gov.educ.pen.nominalroll.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollPostedStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepositoryCustom;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentValidationErrorRepository;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

  NominalRollService service;

  @Mock
  MessagePublisher messagePublisher;

  @Before
  public void before() {
    this.service = new NominalRollService(this.messagePublisher, this.repository, this.postedStudentRepository, this.nominalRollStudentRepositoryCustom, this.nominalRollStudentValidationErrorRepository);
  }

//  @Test
//  public void testRetrieveStudent_WhenStudentExistInDB_ShouldReturnStudent() throws JsonProcessingException {
//    StudentEntity student = service.createStudent(getStudentCreate()).getLeft();
//    assertNotNull(student);
//    assertNotNull(service.retrieveStudent(student.getStudentID()));
//  }

  @Test
  public void testRetrieveStudent_WhenStudentDoesNotExistInDB_ShouldThrowEntityNotFoundException() {
    final var studentID = UUID.fromString("00000000-0000-0000-0000-f3b2d4f20000");
    assertThrows(EntityNotFoundException.class, () -> this.service.getNominalRollStudentByID(studentID));
  }

//  @Test
//  public void testUpdateStudent_WhenPayloadIsValid_ShouldReturnTheUpdatedObject() throws JsonProcessingException {
//
//    StudentEntity student = service.createStudent(getStudentCreate()).getLeft();
//    student.setLegalFirstName("updatedFirstName");
//    var trueStudentID = UUID.randomUUID();
//    student.setTrueStudentID(trueStudentID);
//
//    var studentUpdate = new StudentUpdate();
//    studentUpdate.setStudentID(student.getStudentID().toString());
//    studentUpdate.setHistoryActivityCode("USEREDIT");
//    studentUpdate.setUpdateUser("Test Update");
//    BeanUtils.copyProperties(StudentMapper.mapper.toStructure(student), studentUpdate);
//    StudentEntity updateEntity = service.updateStudent(studentUpdate, UUID.fromString(studentUpdate.getStudentID())).getLeft();
//    assertNotNull(updateEntity);
//    assertThat(updateEntity.getLegalFirstName()).isEqualTo("updatedFirstName".toUpperCase());
//
//    var history = studentHistoryRepository.findByStudentID(student.getStudentID(), PageRequest.of(0, 10));
//    assertThat(history.getTotalElements()).isEqualTo(2);
//    assertThat(history.getContent().get(1).getHistoryActivityCode()).isEqualTo("USEREDIT");
//    assertThat(history.getContent().get(1).getCreateUser()).isEqualTo(studentUpdate.getUpdateUser());
//    assertThat(history.getContent().get(1).getLegalFirstName()).isEqualTo(studentUpdate.getLegalFirstName().toUpperCase());
//    assertThat(history.getContent().get(1).getTrueStudentID()).isEqualTo(trueStudentID);
//
//  }

//  @Test(expected = EntityNotFoundException.class)
//  public void testUpdateStudent_WhenStudentNotExist_ShouldThrowException() throws JsonProcessingException {
//
//    StudentEntity student = getStudentEntity();
//    student.setStudentID(UUID.randomUUID());
//    student.setLegalFirstName("updatedFirstName");
//
//    var studentUpdate = new StudentUpdate();
//    studentUpdate.setStudentID(student.getStudentID().toString());
//    studentUpdate.setHistoryActivityCode("USEREDIT");
//    BeanUtils.copyProperties(StudentMapper.mapper.toStructure(student), studentUpdate);
//    service.updateStudent(studentUpdate, UUID.fromString(studentUpdate.getStudentID()));
//  }

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
