package com.code.block.rest_service.service;

import com.code.block.rest_service.model.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.code.block.rest_service.utils.ResponseUtils.response;

@Slf4j
public class RestRouter extends AbstractVerticle {

    private final JWTAuthHandlerImpl jwtHandler;
    private final MongoDao mongoDao;

    private static final String MISSING_JSON_BODY_MSG = "Json body not included in request";
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";

    public RestRouter(JWTAuthHandlerImpl jwtHandler, MongoDao mongoDao) {
        this.jwtHandler = jwtHandler;
        this.mongoDao = mongoDao;
    }

    public void register(RoutingContext routingContext) {
        JsonObject user = routingContext.body().asJsonObject();
        if (user == null) {
            String message = MISSING_JSON_BODY_MSG;
            log.debug(message);
            response(routingContext, 400, message);
            return;
        }
        String loginValue = user.getString(LOGIN);
        String passwordValue = user.getString(PASSWORD);
        if (passwordValue == null || loginValue == null) {
            String message = "User or password field not present in request JSON";
            log.debug(message);
            response(routingContext, 400, message);
            return;
        }

        Promise<Boolean> isLoginPresentPromise = Promise.promise();
        mongoDao.isLoginPresent(loginValue, isLoginPresentPromise);

        isLoginPresentPromise.future().compose(isPresent -> {
            if (Boolean.TRUE.equals(isPresent)) {
                return Future.failedFuture(new RuntimeException("User login already present"));
            } else {
                JsonObject userToSave = new JsonObject()
                        .put(LOGIN, loginValue)
                        .put(PASSWORD, passwordValue);

                return mongoDao.saveUser(userToSave);
            }
        }).onSuccess(res -> {
            String message = String.format("User: '%s' registered successfully", loginValue);
            log.debug(message);
            response(routingContext, 200, message);
        }).onFailure(error -> {
            String message = "Error while registering user: " + error.getMessage();
            log.warn(message);
            response(routingContext, 500, message);
        });
    }

    public void login(RoutingContext routingContext, JWTAuth provider) {
        JsonObject user = routingContext.body().asJsonObject();

        if (user == null) {
            String message = MISSING_JSON_BODY_MSG;
            log.debug(message);
            response(routingContext, 400, message);
            return;
        }
        String loginValue = user.getString(LOGIN);
        String passwordValue = user.getString(PASSWORD);
        if (passwordValue == null || loginValue == null) {
            String message = "User or password field not present in request JSON";
            log.debug(message);
            response(routingContext, 400, message);
            return;
        }

        Promise<Optional<User>> isUserPresentPromise = Promise.promise();
        mongoDao.getUser(loginValue, passwordValue, isUserPresentPromise);

        isUserPresentPromise.future().compose(optionalUser ->
                        optionalUser.map(Future::succeededFuture).orElseGet(() -> Future.failedFuture(new RuntimeException("User login or password incorrect"))))
                .onSuccess(res -> {

                    log.debug("Generating token for user with id: {}", res.getId());
                    String token = provider.generateToken(
                            new JsonObject().put("_id", res.getId()), new JWTOptions());

                    response(routingContext, 200, "Token obtained successfully", new JsonObject().put("token", token).encode());
                })
                .onFailure(error -> {
                    String message = "Error while logging: " + error.getMessage();
                    log.warn(message);
                    response(routingContext, 500, message);
                });
    }

    public void saveItem(RoutingContext routingContext) {
        jwtHandler.authenticate(routingContext, res -> {
            if (res.succeeded()) {
                String userId = res.result().principal().getString("_id");
                log.debug("User with id: {} successfully authenticated", userId);
                saveItem(routingContext, userId);
            } else {
                String message = "Unauthenticated to preform action";
                log.debug(message);
                response(routingContext, 403, message);
            }
        });
    }

    private void saveItem(RoutingContext routingContext, String userId) {
        if (userId == null) {
            String message = "User Id not present in token";
            log.debug(message);
            response(routingContext, 400, message);
            return;
        }

        JsonObject title = routingContext.body().asJsonObject();
        if (title == null) {
            String message = MISSING_JSON_BODY_MSG;
            log.debug(message);
            response(routingContext, 400, message);
            return;
        }

        String titleValue = title.getString("title");
        if (titleValue == null) {
            String message = "Title field not present in request JSON";
            log.debug(message);
            response(routingContext, 400, message);
            return;
        }

        JsonObject titleToSave = new JsonObject()
                .put("title", titleValue)
                .put("userId", userId);

        mongoDao.saveTitle(titleToSave)
                .onSuccess(id -> {
                    log.info("Item: '{}' saved successfully with id: {}", titleValue, id);
                    response(routingContext, 204, "Item created successfully");
                })
                .onFailure(throwable -> {
                    log.error("Item failed to save: {}", throwable.getMessage());
                    response(routingContext, 500, "Item failed to save");
                });
    }

    public void getTitles(RoutingContext routingContext) {
        jwtHandler.authenticate(routingContext, res -> {
            if (res.succeeded()) {
                String userId = res.result().principal().getString("_id");
                log.debug("User with id: {} successfully authenticated", userId);
                getTitles(routingContext, userId);
            } else {
                String message = "Unauthenticated to preform action";
                log.debug(message);
                response(routingContext, 403, message);
            }
        });
    }

    private void getTitles(RoutingContext routingContext, String userId) {
        if (userId == null) {
            String message = "User Id not present in token";
            log.debug(message);
            response(routingContext, 400, message);
            return;
        }

        mongoDao.getTitles(userId)
                .onSuccess(res -> response(routingContext, 200, "Items successfully retrieved", res.toString()))
                .onFailure(throwable -> {
                    log.error("Items failed to get: {}", throwable.getMessage());
                    response(routingContext, 500, "Items failed to get");
                });
    }
}
