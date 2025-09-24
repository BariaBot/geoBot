package com.example.dating.backend.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeometryConfig {

  private static final int SRID_4326 = 4326;

  @Bean
  public GeometryFactory geometryFactory() {
    return new GeometryFactory(new PrecisionModel(), SRID_4326);
  }
}
