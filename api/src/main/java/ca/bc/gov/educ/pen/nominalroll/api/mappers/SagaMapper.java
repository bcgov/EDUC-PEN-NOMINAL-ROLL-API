package ca.bc.gov.educ.pen.nominalroll.api.mappers;


import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.SagaEventStates;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Saga mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface SagaMapper {
  /**
   * The constant mapper.
   */
  SagaMapper mapper = Mappers.getMapper(SagaMapper.class);

  /**
   * To struct ca . bc . gov . educ  . pen . nominalroll . api . struct . v 1 . saga.
   *
   * @param saga the saga
   * @return the ca . bc . gov . educ . pen . nominalroll . api . struct . v 1 . saga
   */
  ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Saga toStruct(Saga saga);

  /**
   * To model saga.
   *
   * @param struct the struct
   * @return the saga
   */
  Saga toModel(ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Saga struct);

  @Mapping(target = "sagaId", source = "saga.sagaId")
  ca.bc.gov.educ.pen.nominalroll.api.struct.v1.SagaEvent toEventStruct(SagaEventStates sagaEvent);
}
