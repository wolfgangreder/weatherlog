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
import java.time.LocalDate;
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
@Entity(name = "HeatpumpEnergy")
@Table(name = "heatpumpenergy")
@SequenceGenerator(name = "heatpumpenergy_seq", sequenceName = "heatpumpenergy_seq", allocationSize = 1)
public class HeatpumpEnergy
{

  @Column(name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "heatpumpenergy_seq")
  @Id
  @JsonbTransient
  private long id;
  @EqualsAndHashCode.Include
  @Column(name = "sampleday", updatable = false)
  private LocalDate sampleday;
  @Column(name = "earnedEnvironmentEnergyHeating")
  private double earnedEnvironmentEnergyHeating;
  @Column(name = "consumedElectricalEnergyDomesticHotWater")
  private double consumedElectricalEnergyDomesticHotWater;
  @Column(name = "consumedElectricalEnergyHeating")
  private double consumedElectricalEnergyHeating;
  @Column(name = "heatGeneratedHeating")
  private double heatGeneratedHeating;
  @Column(name = "earnedEnvironmentEnergyDomesticHotWater")
  private double earnedEnvironmentEnergyDomesticHotWater;
  @Column(name = "heatGeneratedDomesticHotWater")
  private double heatGeneratedDomesticHotWater;

}