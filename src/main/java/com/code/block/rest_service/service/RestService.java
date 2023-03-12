package com.code.block.rest_service.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestService extends AbstractVerticle {
  public static final int HTTP_SERVER_PORT = 8888;

  @Override
  public void start(Promise<Void> startPromise) {
    configureHttpServer(startPromise, getMongoClient());
  }

  private void configureHttpServer(Promise<Void> startPromise, MongoClient mongoClient) {
    JWTAuthOptions config = new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions()
        .setPath("keystore.jceks")
        .setPassword("password"));

    JWTAuth jwtProvider = JWTAuth.create(vertx, config);
    JWTAuthHandlerImpl jwtHandler = new JWTAuthHandlerImpl(jwtProvider, null);

    RestRouter restRouter = new RestRouter();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/items").handler(requestContext -> restRouter.saveItem(requestContext, mongoClient, jwtHandler));
    router.get("/items").handler(requestContext -> restRouter.getItems(requestContext, mongoClient, jwtHandler));
    router.post("/register").handler(requestContext -> restRouter.register(requestContext, mongoClient));
    router.post("/login").handler(requestContext -> restRouter.login(requestContext, mongoClient, jwtProvider));

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(HTTP_SERVER_PORT)
      .onSuccess(server -> {
          log.info("Server is running on port: {}", HTTP_SERVER_PORT);
          startPromise.complete();
        }
      )
      .onFailure(startPromise::fail);
  }

  private MongoClient getMongoClient() {
    JsonObject mongoConfig = new JsonObject()
      .put("connection_string", "mongodb://localhost:27017")
      .put("db_name", "test");

    return MongoClient.create(vertx, mongoConfig);
  }
}