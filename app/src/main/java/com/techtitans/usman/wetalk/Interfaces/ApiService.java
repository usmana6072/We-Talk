package com.techtitans.usman.wetalk.Interfaces;

import com.techtitans.usman.wetalk.Services.NotificationSender;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @Headers({"Content-Type: application/json"})
    @POST("v1/projects/{projectId}/messages:send")
    Call<ResponseBody> sendNotification(
            @Path("projectId") String projectId,
            @Header("Authorization") String authorizationHeader,
            @Body NotificationSender body
    );
}
