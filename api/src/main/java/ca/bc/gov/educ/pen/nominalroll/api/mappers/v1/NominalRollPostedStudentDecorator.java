package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.sld.v1.SldDiaStudent;
import lombok.val;

import static ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper.YYYY_MM_DD_FORMATTER;

public abstract class NominalRollPostedStudentDecorator implements NominalRollPostedStudentMapper {
  private final NominalRollPostedStudentMapper delegate;

  protected NominalRollPostedStudentDecorator(final NominalRollPostedStudentMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public SldDiaStudent toDiaStudent(final NominalRollPostedStudentEntity nominalRollPostedStudentEntity, RestUtils restUtils) {
    val sldDiaStudent = this.delegate.toDiaStudent(nominalRollPostedStudentEntity, restUtils);
    sldDiaStudent.setSchtype(NominalRollHelper.getSldSchTypeMap().get(nominalRollPostedStudentEntity.getAgreementType()));
    sldDiaStudent.setStudBirth(nominalRollPostedStudentEntity.getBirthDate().format(YYYY_MM_DD_FORMATTER));
    sldDiaStudent.setFteVal(nominalRollPostedStudentEntity.getFte().longValue());
    sldDiaStudent.setReportDate(Long.parseLong(nominalRollPostedStudentEntity.getProcessingYear().format(YYYY_MM_DD_FORMATTER)));
    val mincode = restUtils.getFedProvSchoolCodes().get(nominalRollPostedStudentEntity.getFederalSchoolNumber());
    sldDiaStudent.setDistNo(mincode.substring(0, 3));
    sldDiaStudent.setSchlNo(mincode.substring(3));
    return sldDiaStudent;
  }
}
