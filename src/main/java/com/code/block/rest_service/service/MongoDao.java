package com.code.block.rest_service.service;

import com.code.block.rest_service.model.User;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class MongoDao {

  private final MongoClient mongoClient;

  public MongoDao(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  public void isLoginPresent(String login, Promise<Boolean> promise) {
    JsonObject query = new JsonObject().put("login", login);
    mongoClient.findOne("users", query, new JsonObject(), res -> {
      if (res.succeeded()) {
        promise.complete(res.result() != null);
      } else {
        promise.fail(res.cause());
      }
    });
  }

  public void getUser(String login, String password, Promise<Optional<User>> promise) {
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

  public Future<Void> saveUser(JsonObject user) {
    Promise<Void> promise = Promise.promise();
    mongoClient.save("users", user, res -> {
      if (res.succeeded()) {
        promise.complete();
      } else {
        promise.fail(res.cause());
      }
    });
    return promise.future();
  }

  public Future<String> saveTitle(JsonObject title) {
    Promise<String> promise = Promise.promise();
    mongoClient.insert("titles", title, res -> {
      if (res.succeeded()) {
        promise.complete(res.result());
      } else {
        promise.fail(res.cause());
      }
    });
    return promise.future();
  }

  public Future<List<JsonObject>> getTitles(String userId) {
    Promise<List<JsonObject>> promise = Promise.promise();
    JsonObject query = new JsonObject().put("userId", userId);
    FindOptions findOptions = new FindOptions().setFields(new JsonObject().put("userId", 0));
    mongoClient.findWithOptions("titles", query, findOptions, res -> {
      if (res.succeeded()) {
        promise.complete(res.result());
      } else {
        promise.fail(res.cause());
      }
    });
    return promise.future();
  }
}
