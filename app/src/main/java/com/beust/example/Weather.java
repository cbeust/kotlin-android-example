package com.beust.example;

import com.google.gson.JsonObject;

import retrofit.http.GET;
import rx.Observable;

/**
 * @author Cedric Beust <cedric@refresh.io>
 * @since 02 16, 2015
 */
public interface Weather {
    @GET("/data/2.5/weather?q=London,uk")
    Observable<JsonObject> weather();
}
