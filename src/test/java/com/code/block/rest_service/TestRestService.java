package com.code.block.rest_service;

import com.code.block.rest_service.model.User;
import com.code.block.rest_service.repository.MongoDao;
import com.code.block.rest_service.service.RestRouter;
import com.code.block.rest_service.service.RestService;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class TestRestService {

    @Mock
    MongoDao mongoDao;

    @BeforeEach
    void setup(Vertx vertx, VertxTestContext testContext) {
        RestRouter restRouter = new RestRouter(getJwtHandler(), getJwtProvider(), mongoDao);
        RestService restService = new RestService(restRouter);
        vertx.deployVerticle(restService, testContext.succeeding(id -> testContext.completeNow()));
    }

    @Test
    void shouldFailRegisteringWhenBodyNotPresent(Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //then
        client.request(HttpMethod.POST, 8888, "localhost", "/register")
                .compose(req -> req.send("").compose(response -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("Json body not included in request", response.statusMessage());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    @ParameterizedTest
    @CsvSource({
            "userr, password, /register",
            ", password, /register",
            "user, , /register",
            "user, pass, /register",
            "userr, password, /login",
            ", password, /login",
            "user, , /login",
            "user, pass, /login"
    })
    void shouldFailRegisteringAndLogingWhenFieldNotIncluded(String user, String password, String path, Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //then
        client.request(HttpMethod.POST, 8888, "localhost", path)
                .compose(req -> req.send("{\"" + user + "\": \"user\", \"" + password + "\": \"pass\"}").compose(response -> {
                    assertEquals(400, response.statusCode());
                    assertEquals("User or password field not present in request JSON", response.statusMessage());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void shouldNotCreateAlreadyExistingLogin(Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //when
        Mockito.when(mongoDao.isLoginPresent(any())).thenReturn(Future.succeededFuture(true));

        //then
        client.request(HttpMethod.POST, 8888, "localhost", "/register")
                .compose(req -> req.send("{\"login\": \"user\", \"password\": \"pass\"}").compose(response -> {
                    assertEquals(500, response.statusCode());
                    assertEquals("Error while registering user: User login already present", response.statusMessage());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void shouldRegister(Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //when
        Mockito.when(mongoDao.isLoginPresent(any())).thenReturn(Future.succeededFuture(Boolean.FALSE));
        Mockito.when(mongoDao.saveUser(any())).thenReturn(Future.succeededFuture());

        //then
        client.request(HttpMethod.POST, 8888, "localhost", "/register")
                .compose(req -> req.send("{\"login\": \"user\", \"password\": \"pass\"}").compose(response -> {
                    assertEquals(200, response.statusCode());
                    assertEquals("User: 'user' registered successfully", response.statusMessage());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void shouldNotLoginIfUserNotPresent(Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //when
        Mockito.when(mongoDao.getUser(any(), any())).thenReturn(Future.succeededFuture(Optional.empty()));

        //then
        client.request(HttpMethod.POST, 8888, "localhost", "/login")
                .compose(req -> req.send("{\"login\": \"user\", \"password\": \"pass\"}").compose(response -> {
                    assertEquals(500, response.statusCode());
                    assertEquals("Error while logging: User login or password incorrect", response.statusMessage());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    @Test
    void shouldLogin(Vertx vertx, VertxTestContext testContext) {
        //given
        HttpClient client = vertx.createHttpClient();

        //when
        Mockito.when(mongoDao.getUser(any(), any())).thenReturn(Future.succeededFuture(Optional.of(User.builder().login("login").password("password").id("777").build())));

        //then
        client.request(HttpMethod.POST, 8888, "localhost", "/login")
                .compose(req -> req.send("{\"login\": \"user\", \"password\": \"pass\"}").compose(response -> {
                    assertEquals(200, response.statusCode());
                    assertEquals("Token obtained successfully", response.statusMessage());
                    assertNotNull(response.body());
                    return Future.succeededFuture(response.statusMessage());
                }))
                .onComplete(testContext.succeeding(buffer -> testContext.verify(testContext::completeNow)));
    }

    private JWTAuthOptions getJWTConfig() {
        return new JWTAuthOptions()
                .setKeyStore(new KeyStoreOptions()
                        .setPath("./src/test/resources/keystore.jceks")
                        .setPassword("password"));
    }

    private JWTAuth getJwtProvider() {
        return JWTAuth.create(Vertx.vertx(), getJWTConfig());
    }

    private JWTAuthHandlerImpl getJwtHandler() {
        return new JWTAuthHandlerImpl(getJwtProvider(), null);
    }
}
