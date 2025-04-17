package ca.bc.gov.educ.pen.nominalroll.api.struct.v1;

import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class DownloadableReportResponse implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String reportType;

  @ToString.Exclude
  private String documentData;

}
