/*
 * Copyright 2024 Wolfgang Reder.
 * Licensed under the Apache License, Version 2.0
 */
package at.or.reder.weather.rest;

import at.or.reder.weather.monitoring.DatabaseMonitoringService;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.Reader;
import java.sql.Connection;
import java.util.Map;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

@ApplicationScoped
@Path("system")
public class SystemResource {

    @Inject
    @DataSource("weather")
    AgroalDataSource ds;

    @Inject
    DatabaseMonitoringService monitoringService;

    @PUT
    @Path("updateDatabaseMetadata")
    public Response updateDatabaseMetadata() throws Exception {
        Scope.child(Map.of(), () -> {
            try (Connection connection = ds.getConnection()) {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                Liquibase liquibase = new Liquibase(
                        "at/or/reder/weather/jpa/changelog_0.0.1.xml",
                        new ClassLoaderResourceAccessor(getClass().getClassLoader()),
                        database);
                liquibase.update(new Contexts(), new LabelExpression());
            }
        });
        return Response.noContent().build();
    }

    @GET
    @Path("metrics/database")
    public Response fetchDatabaseMetrics() {
        Reader reader = monitoringService.fetchDatabaseMetrics();
        return Response.ok(reader, MediaType.TEXT_PLAIN).build();
    }
}
