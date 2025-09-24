package com.example.dating.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class PostgisIntegrationTest {

    private static final DockerImageName POSTGIS_IMAGE =
        DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres");

    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/init-postgis.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
    }
}
