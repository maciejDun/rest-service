package com.code.block.rest_service.service;

import com.code.block.rest_service.model.User;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UsersService {

  public static void isLoginPresent(String login, MongoClient mongoClient, Promise<Boolean> promise) {
    JsonObject query = new JsonObject().put("login", login);
    mongoClient.findOne("users", query, new JsonObject(), res -> {
      if (res.succeeded()) {
        promise.complete(res.result() != null);
      } else {
        promise.fail(res.cause());
      }
    });
  }

  public static void getUser(String login, String password, MongoClient mongoClient, Promise<Optional<User>> promise) {
    JsonObject query = new JsonObject().put("login", login).put("password", password);
    mongoClient.findOne("users", query, new JsonObject(), res -> {
      if (res.succeeded()) {
        if (res.result() != null) {
          promise.complete(Optional.ofNullable(
            User.builder()
            .id(res.result().getString("_id"))
            .login("login")
            .password(res.result().getString("password"))
            .build()
          ));
        } else {
          promise.complete(Optional.empty());
        }
      } else {
        promise.fail(res.cause());
      }
    });
  }
}
