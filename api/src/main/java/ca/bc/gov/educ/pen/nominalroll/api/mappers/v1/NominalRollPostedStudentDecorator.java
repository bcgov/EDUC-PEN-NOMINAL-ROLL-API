package ca.bc.gov.educ.pen.nominalroll.api.mappers.v1;

import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.sld.v1.SldDiaStudent;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

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
    if(StringUtils.isNotBlank(nominalRollPostedStudentEntity.getFederalRecipientBandName()) && nominalRollPostedStudentEntity.getFederalRecipientBandName().length() > 20){
      sldDiaStudent.setBandname(nominalRollPostedStudentEntity.getFederalRecipientBandName().substring(0, 20));
    }else{
      sldDiaStudent.setBandname(nominalRollPostedStudentEntity.getFederalRecipientBandName());
    }

    sldDiaStudent.setReportDate(Long.parseLong(nominalRollPostedStudentEntity.getProcessingYear().format(YYYY_MM_DD_FORMATTER)));
    val mincode = restUtils.getFedProvSchoolCodes().get(nominalRollPostedStudentEntity.getFederalSchoolNumber());
    sldDiaStudent.setDistNo(mincode.substring(0, 3));
    sldDiaStudent.setSchlNo(mincode.substring(3));
    return sldDiaStudent;
  }
}
