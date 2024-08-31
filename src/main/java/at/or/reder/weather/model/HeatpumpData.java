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
package at.or.reder.weather.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author Wolfgang Reder
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity(name = "HeatpumpData")
@Table(name = "heatpumpdata")
@SequenceGenerator(name = "heatpumpdata_seq", sequenceName = "heatpumpdata_seq", allocationSize = 1)
public class HeatpumpData
{

  @Column(name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "heatpumpdata_seq")
  @Id
  @JsonbTransient
  private long id;
  @EqualsAndHashCode.Include
  @Column(name = "sampletime", updatable = false)
  private LocalDateTime sampletime;
  @Column(name = "roomTemp")
  private double roomTemp;
  @Column(name = "roomTempSet")
  private double roomTempSet;
  @Column(name = "outdoorTemp")
  private double outdoorTemp;
  @Column(name = "hotWaterTemp")
  private double hotWaterTemp;
}
