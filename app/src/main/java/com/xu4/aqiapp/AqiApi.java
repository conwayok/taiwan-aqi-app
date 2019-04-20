package com.xu4.aqiapp;

import com.xu4.aqiapp.AqiModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface AqiApi {

  @GET
  Call<List<AqiModel>> getAqi(@Url String url);
}
