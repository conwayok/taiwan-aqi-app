package com.xu4.aqiapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AqiModel {

  @JsonProperty("SiteName")
  private String siteName;

  @JsonProperty("County")
  private String county;

  @JsonProperty("AQI")
  private String aqi;

  //  @JsonProperty("Pollutant")
  //  private String pollutant;

  @JsonProperty("Status")
  private String status;

  @JsonProperty("Latitude")
  private String latitude;

  @JsonProperty("Longitude")
  private String longitude;
}
