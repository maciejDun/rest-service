package com.code.block.rest_service.service;

import com.code.block.rest_service.repository.MongoDao;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestService extends AbstractVerticle {
  public static final int HTTP_SERVER_PORT = 8888;

  private final MongoDao mongoDao;
  private final JWTAuthOptions config;

  public RestService(MongoDao mongoDao, JWTAuthOptions config) {
    this.mongoDao = mongoDao;
    this.config = config;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JWTAuth jwtProvider = JWTAuth.create(vertx, config);
    JWTAuthHandlerImpl jwtHandler = new JWTAuthHandlerImpl(jwtProvider, null);

    RestRouter restRouter = new RestRouter(jwtHandler, mongoDao);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/items").handler(restRouter::saveItem);
    router.get("/items").handler(restRouter::getTitles);
    router.post("/register").handler(restRouter::register);
    router.post("/login").handler(requestContext -> restRouter.login(requestContext, jwtProvider));

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
