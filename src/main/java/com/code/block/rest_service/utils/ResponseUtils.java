package com.code.block.rest_service.utils;

import io.vertx.ext.web.RoutingContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResponseUtils {
  public static void response(RoutingContext routingContext, int code, String message) {
    routingContext.response()
      .setStatusCode(code)
      .setStatusMessage(message)
      .end();
  }

  public static void response(RoutingContext routingContext, int code, String message, String body) {
    routingContext.response()
      .setStatusCode(code)
      .setStatusMessage(message)
      .putHeader("Content-Type", "application/json")
      .end(body);
  }
}
