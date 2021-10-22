package ca.bc.gov.educ.pen.nominalroll.api.mappers;


import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import org.mapstruct.Mapper;
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
   * To struct ca . bc . gov . educ . api . pen . services . struct . v 1 . saga.
   *
   * @param saga the saga
   * @return the ca . bc . gov . educ . api . pen . services . struct . v 1 . saga
   */
  ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Saga toStruct(Saga saga);
}
