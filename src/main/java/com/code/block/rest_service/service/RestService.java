package com.code.block.rest_service.service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestService extends AbstractVerticle {
  public static final int HTTP_SERVER_PORT = 8888;

  private final RestRouter restRouter;

  public RestService(RestRouter restRouter) {
    this.restRouter = restRouter;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/items").handler(restRouter::saveItem);
    router.get("/items").handler(restRouter::getTitles);
    router.post("/register").handler(restRouter::register);
    router.post("/login").handler(restRouter::login);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(HTTP_SERVER_PORT)
      .onSuccess(server -> {
          log.info("Server is running on port: {}", HTTP_SERVER_PORT);
          startPromise.complete();
        }
      )
      .onFailure(cause -> {
        log.error("Server failed to start, cause: {}", cause.getMessage());
        startPromise.fail(cause);
      });
  }

}
