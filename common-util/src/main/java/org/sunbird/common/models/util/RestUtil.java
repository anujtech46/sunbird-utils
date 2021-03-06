package org.sunbird.common.models.util;

import akka.dispatch.Futures;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.body.Body;
import com.mashape.unirest.request.body.RequestBodyEntity;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/** @author Mahesh Kumar Gangula */
public class RestUtil {

  private static String basePath;

  static {
    basePath = System.getenv(JsonKey.EKSTEP_BASE_URL);
    if (StringUtils.isBlank(basePath)) {
      basePath = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_BASE_URL);
    }

    String apiKey = System.getenv(JsonKey.EKSTEP_AUTHORIZATION);
    if (StringUtils.isBlank(apiKey)) {
      apiKey = PropertiesCache.getInstance().getProperty(JsonKey.EKSTEP_AUTHORIZATION);
    }
    Unirest.setDefaultHeader("Content-Type", "application/json");
    Unirest.setDefaultHeader("Authorization", "Bearer " + apiKey);
  }

  public static Future<HttpResponse<JsonNode>> executeAsync(BaseRequest request) {
    ProjectLogger.log("RestUtil:execute: request url = " + request.getHttpRequest().getUrl());
    Promise<HttpResponse<JsonNode>> promise = Futures.promise();

    Body body = request.getHttpRequest().getBody();
    if ((body != null) && (body instanceof RequestBodyEntity)) {
      RequestBodyEntity rbody = (RequestBodyEntity) body;
      ProjectLogger.log("RestUtil:execute: request body = " + rbody.getBody());
    }
    request.asJsonAsync(
        new Callback<JsonNode>() {

          @Override
          public void failed(UnirestException e) {
            promise.failure(e);
          }

          @Override
          public void completed(HttpResponse<JsonNode> response) {
            promise.success(response);
          }

          @Override
          public void cancelled() {
            promise.failure(new Exception("cancelled"));
          }
        });

    return promise.future();
  }

  public static HttpResponse<JsonNode> execute(BaseRequest request) throws Exception {
    ProjectLogger.log("RestUtil:execute: request url = " + request.getHttpRequest().getUrl());
    Body body = request.getHttpRequest().getBody();
    if ((body != null) && (body instanceof RequestBodyEntity)) {
      RequestBodyEntity rbody = (RequestBodyEntity) body;
      ProjectLogger.log("RestUtil:execute: request body = " + rbody.getBody());
    }

    HttpResponse<JsonNode> response = request.asJson();
    return response;
  }

  public static String getBasePath() {
    return basePath;
  }

  public static String getFromResponse(HttpResponse<JsonNode> resp, String key) throws Exception {
    String[] nestedKeys = key.split("\\.");
    JSONObject obj = resp.getBody().getObject();

    for (int i = 0; i < nestedKeys.length - 1; i++) {
      String nestedKey = nestedKeys[i];
      if (obj.has(nestedKey)) obj = obj.getJSONObject(nestedKey);
    }

    String val = obj.getString(nestedKeys[nestedKeys.length - 1]);
    return val;
  }

  public static boolean isSuccessful(HttpResponse<JsonNode> resp) {
    int status = resp.getStatus();
    return (status == 200);
  }
}
