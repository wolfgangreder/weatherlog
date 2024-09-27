/*
 * Copyright 2024 wolfi.
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
package at.or.reder.weather.service;

import at.or.reder.weather.model.HeatpumpEnergyRecord;
import at.or.reder.weather.model.HeatpumpScope;
import java.io.IOException;
import java.io.LineNumberReader;
import java.time.LocalDate;
import java.util.Optional;

public interface HeatpumpService {

  public void insertSystemData(LineNumberReader reader) throws IOException;

  public void insertZoneData(LineNumberReader reader) throws IOException;

  public void insertHotWaterData(LineNumberReader reader) throws IOException;

  public void insertEnergyData(LineNumberReader reader) throws IOException;

  public Optional<HeatpumpEnergyRecord> getEnergy(HeatpumpScope scope, LocalDate day);
}
