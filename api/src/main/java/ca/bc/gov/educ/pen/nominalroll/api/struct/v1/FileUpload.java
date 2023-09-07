package ca.bc.gov.educ.pen.nominalroll.api.struct.v1;

import lombok.*;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUpload {
  @NotNull
  String fileExtension;
  @NotNull
  String createUser;
  @NotNull
  String updateUser;
  @NotNull
  @ToString.Exclude
  String fileContents;
}
