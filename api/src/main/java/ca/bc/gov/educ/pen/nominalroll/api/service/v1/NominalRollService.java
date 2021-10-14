package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudent;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NominalRollService {
  private final NominalRollStudentRepository repository;

  public NominalRollService(final NominalRollStudentRepository repository) {
    this.repository = repository;
  }

  public boolean isAllRecordsProcessed() {
    final long count = this.repository.countByStatus(NominalRollStudentStatus.LOADED.toString());
    return count < 1;
  }

  public boolean isCurrentYearFileBeingProcessed() {
    return this.repository.count() > 0;
  }

  public List<NominalRollStudent> getAllNominalRollStudents() {
    return this.repository.findAll();
  }

  public Optional<NominalRollStudent> getNominalRollStudentByID(final UUID nominalRollStudentID) {
    return this.repository.findById(nominalRollStudentID);
  }

  public void deleteAllNominalRollStudents() {
    this.repository.deleteAll();
  }
}
