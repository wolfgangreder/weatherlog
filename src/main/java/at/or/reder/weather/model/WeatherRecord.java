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
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity(name = "WeatherRecord")
@Table(name = "stationdata")
@SequenceGenerator(name = "stationdata_seq", sequenceName = "stationdata_seq", allocationSize = 1)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WeatherRecord
{
  
  @Column(name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stationdata_seq")
  @Id
  @JsonbTransient
  private long id;
  @Column(name = "stationkey", length = 32, updatable = false)
  @JsonbTransient
  private String stationkey;
  @Transient
  @JsonbTransient
  private String stationtype;
  @Transient
  @JsonbTransient
  private long runtime;
  @Transient
  @JsonbTransient
  private long heap;
  @EqualsAndHashCode.Include
  @Column(name = "sampletime", updatable = false)
  private LocalDateTime sampleTime;
  @Column(name = "tempin", updatable = false)
  private Double tempin;
  @Column(name = "tempout", updatable = false)
  private Double tempout;
  @Column(name = "humidityin", updatable = false)
  private Double humidityin;
  @Column(name = "humidityout", updatable = false)
  private Double humidityout;
  @Column(name = "pressurerel", updatable = false)
  private Double pressurerel;
  @Column(name = "pressureabs", updatable = false)
  private Double pressureabs;
  @Column(name = "winddir", updatable = false)
  private Integer winddir;
  @Column(name = "windspeed", updatable = false)
  private Double windspeed;
  @Column(name = "windgust", updatable = false)
  private Double windgust;
  @Column(name = "maxdailygust", updatable = false)
  private Double maxdailygust;
  @Column(name = "solarradiation", updatable = false)
  private Double solarradiation;
  @Column(name = "uv", updatable = false)
  private Integer uv;
  @Column(name = "rainrate", updatable = false)
  private Double rainrate;
  @Column(name = "eventrain", updatable = false)
  private Double eventrain;
  @Column(name = "hourlyrain", updatable = false)
  private Double hourlyrain;
  @Column(name = "dailyrain", updatable = false)
  private Double dailyrain;
  @Column(name = "weeklyrain", updatable = false)
  private Double weeklyrain;
  @Column(name = "monthlyrain", updatable = false)
  private Double monthlyrain;
  @Column(name = "yearlyrain", updatable = false)
  private Double yearlyrain;
  @Column(name = "totalrain", updatable = false)
  private Double totalrain;
  @Transient
  @JsonbTransient
  private int wh65batt;
  @Transient
  @JsonbTransient
  private int freq;
  @Transient
  @JsonbTransient
  private String model;
  @Transient
  @JsonbTransient
  private int interval;
    
}
