package com.code.block.rest_service;

import com.code.block.rest_service.repository.MongoDao;
import com.code.block.rest_service.service.RestRouter;
import com.code.block.rest_service.service.RestService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class TestRestServiceWithAuth {

    @Mock
    MongoDao mongoDao;
    @Mock
    JWTAuth authProvider;
    @Mock
    JWTAuthHandlerImpl authHandler;

    @BeforeEach
    void setup(Vertx vertx, VertxTestContext testContext) {
        RestRouter restRouter = new RestRouter(authHandler, authProvider, mongoDao);
        RestService restService = new RestService(restRouter);
        vertx.deployVerticle(restService, testContext.succeeding(id -> testContext.completeNow()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "GET"})
    void shouldNotSaveOrGetItemWhenIdNotPresentInToken(String httpMethod, Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //when
        Mockito.doAnswer(invocation -> {
            Handler<AsyncResult<UserImpl>> handler = invocation.getArgument(1);
            handler.handle(Future.succeededFuture(new UserImpl(new JsonObject(), new JsonObject())));
            return null;
        }).when(authHandler).authenticate(Mockito.any(), Mockito.any());

        //then
        client.request(HttpMethod.valueOf(httpMethod), 8888, "localhost", "/items")
                .compose(req -> req.send("{\"title\": \"title22\"}").compose(response -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("User Id not present in token", response.statusMessage());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void shouldNotSaveItemWhenItemNotPresentInBody(Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //when
        Mockito.doAnswer(invocation -> {
            Handler<AsyncResult<UserImpl>> handler = invocation.getArgument(1);
            handler.handle(Future.succeededFuture(new UserImpl(new JsonObject().put("_id", "111"), new JsonObject())));
            return null;
        }).when(authHandler).authenticate(Mockito.any(), Mockito.any());

        //then
        client.request(HttpMethod.POST, 8888, "localhost", "/items")
                .compose(req -> req.send().compose(response -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("Json body not included in request", response.statusMessage());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void shouldNotSaveItemWhenTitleFieldNotPresentInBody(Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //when
        Mockito.doAnswer(invocation -> {
            Handler<AsyncResult<UserImpl>> handler = invocation.getArgument(1);
            handler.handle(Future.succeededFuture(new UserImpl(new JsonObject().put("_id", "111"), new JsonObject())));
            return null;
        }).when(authHandler).authenticate(Mockito.any(), Mockito.any());

        //then
        client.request(HttpMethod.POST, 8888, "localhost", "/items")
                .compose(req -> req.send("{\"wrongField\": \"title22\"}").compose(response -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("Title field not present in request JSON", response.statusMessage());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void shouldSaveItem(Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //when
        Mockito.when(mongoDao.saveTitle(any())).thenReturn(Future.succeededFuture());

        Mockito.doAnswer(invocation -> {
            Handler<AsyncResult<UserImpl>> handler = invocation.getArgument(1);
            handler.handle(Future.succeededFuture(new UserImpl(new JsonObject().put("_id", "111"), new JsonObject())));
            return null;
        }).when(authHandler).authenticate(Mockito.any(), Mockito.any());

        //then
        client.request(HttpMethod.POST, 8888, "localhost", "/items")
                .compose(req -> req.send("{\"title\": \"title22\"}").compose(response -> {
                    assertEquals(204, response.statusCode());
                    assertEquals("Item created successfully", response.statusMessage());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void shouldGetItems(Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();
        List<JsonObject> itemsList = Arrays.asList(new JsonObject().put("_id", "111").put("title", "title1"), new JsonObject().put("_id", "112").put("title", "title2"));

        //when
        Mockito.when(mongoDao.getTitles(any())).thenReturn(Future.succeededFuture(itemsList));

        Mockito.doAnswer(invocation -> {
            Handler<AsyncResult<UserImpl>> handler = invocation.getArgument(1);
            handler.handle(Future.succeededFuture(new UserImpl(new JsonObject().put("_id", "1111"), new JsonObject())));
            return null;
        }).when(authHandler).authenticate(Mockito.any(), Mockito.any());

        //then
        client.request(HttpMethod.GET, 8888, "localhost", "/items")
                .compose(req -> req.send().compose(response -> {
                    assertEquals(200, response.statusCode());
                    assertEquals("Items successfully retrieved", response.statusMessage());
                    return response.body();
                }))
                .onComplete(testContext.succeeding(body -> {
                    assertNotNull(body.toJsonArray());
                    testContext.completeNow();
                }));
    }

}
