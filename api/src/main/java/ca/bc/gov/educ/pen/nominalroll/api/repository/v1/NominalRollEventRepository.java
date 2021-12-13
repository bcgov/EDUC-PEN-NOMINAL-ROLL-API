package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NominalRollEventRepository extends JpaRepository<NominalRollEvent, UUID> {
  List<NominalRollEvent> findAllByCreateDateBefore(LocalDateTime createDate);

  Optional<NominalRollEvent> findBySagaIdAndEventType(UUID sagaId, String eventType);
}
