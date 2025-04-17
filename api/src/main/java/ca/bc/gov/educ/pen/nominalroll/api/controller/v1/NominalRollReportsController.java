package ca.bc.gov.educ.pen.nominalroll.api.controller.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.MinistryReportTypeCode;
import ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1.NominalRollReportsEndpoint;
import ca.bc.gov.educ.pen.nominalroll.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.errors.ApiError;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.AssignedPenEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.PaginatedResponse;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.CSVReportService;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.sdc.v1.Collection;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.DownloadableReportResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
@RequiredArgsConstructor
public class NominalRollReportsController implements NominalRollReportsEndpoint {

    private final CSVReportService ministryReportsService;

    private final NominalRollService nominalRollService;
    private final RestUtils restUtils;



    @Override
    public DownloadableReportResponse getMinistryDownloadableReport(String processingYear) throws JsonProcessingException, ExecutionException, InterruptedException {
        long startTime = System.currentTimeMillis();
        // Run both calls asynchronously
        CompletableFuture<PaginatedResponse<Collection>> collectionsFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            PaginatedResponse<Collection> result = null;
            try {
                result = restUtils.getCollections(processingYear);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            long end = System.currentTimeMillis();
            System.out.println("Time taken to fetch collections: " + (end - start) + " ms");
            return result;
        });

        CompletableFuture<List<AssignedPenEntity>> postedStudentsFuture = CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            List<AssignedPenEntity> result = nominalRollService.findAssignedPenFromPostedStudentsByProcessingYear(processingYear);
            long end = System.currentTimeMillis();
            System.out.println("Time taken to fetch posted students: " + (end - start) + " ms");
            return result;
        });
        PaginatedResponse<Collection> collections = collectionsFuture.join();

        long endTime = System.currentTimeMillis();
        System.out.println("Total time for both async operations: " + (endTime - startTime) + " ms");
        DownloadableReportResponse reportResponse = new DownloadableReportResponse();
            var collection = collections.getContent().get(0);
            if(collection.getCollectionStatusCode().equalsIgnoreCase("COMPLETED")){
                reportResponse= ministryReportsService.generateAllNominalRollStudentService(collection.getCollectionID(),postedStudentsFuture.join(),processingYear);
            }



        return reportResponse;
    }
}
