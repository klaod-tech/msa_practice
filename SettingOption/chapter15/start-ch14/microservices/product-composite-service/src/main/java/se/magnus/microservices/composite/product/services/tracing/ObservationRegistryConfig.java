package se.magnus.microservices.composite.product.services.tracing;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;  // 교재와 다름 (교재: org.springframework.boot.actuate.autoconfigure.observation, Boot 4)
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ObservationRegistryConfig implements ObservationRegistryCustomizer<ObservationRegistry> {

  private final BuildProperties buildProperties;

  public ObservationRegistryConfig(BuildProperties buildProperties) {
    this.buildProperties = buildProperties;
  }

  @Override
  public void customize(final ObservationRegistry registry) {
    registry.observationConfig().observationFilter(new BuildInfoObservationFilter(buildProperties));
  }
}
