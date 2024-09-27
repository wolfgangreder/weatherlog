/*
 * Copyright 2024 Wolfgang Reder.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.or.reder.weather.rest;

import at.or.reder.weather.model.HeatpumpEnergyRecord;
import at.or.reder.weather.model.HeatpumpScope;
import at.or.reder.weather.service.HeatpumpService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.time.LocalDate;
import java.util.Collection;

/**
 *
 * @author Wolfgang Reder
 */
@Path("/heatpump")
@ApplicationScoped
public class HeatPumpResource {

  @Inject
  private HeatpumpService heatpumpService;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Transactional(Transactional.TxType.REQUIRED)
  public Response uploadData(@Context HttpServletRequest request) throws ServletException, IOException
  {
    Collection<Part> parts = request.getParts();
    for (Part part : parts) {
      String filename = part.getSubmittedFileName();
      try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(part.getInputStream()))) {
        if (filename.startsWith("domestic_hot_water")) {
          heatpumpService.insertHotWaterData(reader);
        } else if (filename.startsWith("system")) {
          heatpumpService.insertSystemData(reader);
        } else if (filename.startsWith("zone")) {
          heatpumpService.insertZoneData(reader);
        } else if (filename.startsWith("energy")) {
          heatpumpService.insertEnergyData(reader);
        }
      }
    }
    return Response.noContent().build();
  }

  @GET
  @Path("energy/{scope:heating|water}")
  public Response getEnergyData(@QueryParam("start") String strDateFrom,
          @QueryParam("end") String strDateTo,
          @PathParam("scope") String scope)
  {
    return Response.noContent().build();
  }

  @GET
  @Path("energy/{scope:heating|water}/{day:\\d{4}-\\d{1,2}-\\d{1,2}}")
  @Produces(MediaType.APPLICATION_JSON)
  public HeatpumpEnergyRecord getEnergy(@PathParam("scope") HeatpumpScope scope,
          @PathParam("day") String strDay)
  {
    return heatpumpService.getEnergy(scope, LocalDate.parse(strDay)).orElse(null);
  }
}
