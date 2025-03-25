package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.CacheNames;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheService {
  private final RestUtils restUtils;
  private final CacheManager cacheManager;

  public CacheService(final RestUtils restUtils, final ApplicationProperties applicationProperties, final CacheManager cacheManager) {
    this.restUtils = restUtils;
    this.cacheManager = cacheManager;
    if (applicationProperties.getIsHttpRampUp()) {
      val genders = this.restUtils.getActiveGenderCodes();
      log.info("cached {} genders", genders.size());

      val grades = this.restUtils.getActiveGradeCodes();
      log.info("cached {} grades", grades.size());

      val schools = this.restUtils.getSchools();
      log.info("cached {} schools", schools.size());
      val districts = this.restUtils.districtCodes();
      log.info("cached {} districts", districts.size());
    }
  }

  @Scheduled(cron = "0 0 0 * * *") // at midnight reset cache.
  public void evictAllCacheValues() {
    this.cacheManager.getCacheNames().forEach(cacheName -> {
      val cached = this.cacheManager.getCache(cacheName);
      if (cached != null) {
        cached.clear();
      }
    });
    this.restUtils.getActiveGenderCodes();
    this.restUtils.getActiveGradeCodes();
    this.restUtils.getSchools();
    this.restUtils.districtCodes();
    log.debug("Cache refreshed successfully");
  }

  public void evictCache(@NonNull final String cacheName) {
    val cached = this.cacheManager.getCache(cacheName);
    if (cached != null) {
      cached.clear();
    }
    switch (cacheName) {
      case CacheNames.SCHOOL_CODES:
        this.restUtils.getSchools();
        this.restUtils.districtCodes();
        break;
      case CacheNames.GRADE_CODES:
        this.restUtils.getActiveGradeCodes();
        break;
      case CacheNames.GENDER_CODES:
        this.restUtils.getActiveGenderCodes();
        break;
      case CacheNames.DISTRICT_CODES:
        this.restUtils.districtCodes();
        break;
    }
    log.info("{} Cache was evicted on demand", cacheName);
  }
}
