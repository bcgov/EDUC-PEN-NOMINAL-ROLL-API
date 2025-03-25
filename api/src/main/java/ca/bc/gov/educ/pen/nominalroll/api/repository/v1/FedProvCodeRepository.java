package ca.bc.gov.educ.pen.nominalroll.api.repository.v1;


import ca.bc.gov.educ.pen.nominalroll.api.model.v1.FedProvCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface FedProvCodeRepository extends JpaRepository<FedProvCodeEntity, UUID> {

    void deleteAllBySchoolID(UUID SchoolID);


}
