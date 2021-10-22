package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

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
      this.restUtils.getActiveGenderCodes();
      this.restUtils.getActiveGradeCodes();
      this.restUtils.getFedProvSchoolCodes();
      this.restUtils.getSchools();
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
    this.restUtils.getFedProvSchoolCodes();
    this.restUtils.getSchools();
    log.debug("Cache refreshed successfully");
  }

  public void evictCache(@NonNull final String cacheName) {
    val cached = this.cacheManager.getCache(cacheName);
    if (cached != null) {
      cached.clear();
    }
    log.info("{} Cache was evicted on demand", cacheName);
  }
}
