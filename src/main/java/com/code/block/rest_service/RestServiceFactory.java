package com.code.block.rest_service;

import com.code.block.rest_service.repository.MongoDao;
import com.code.block.rest_service.service.RestRouter;
import com.code.block.rest_service.service.RestService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;

public class RestServiceFactory {
    public RestService buildRestService() {
        return new RestService(new RestRouter(getJwtHandler(), getJwtProvider(), new MongoDao(getMongoClient())));
    }

    private MongoClient getMongoClient() {
        JsonObject mongoConfig = new JsonObject()
                .put("connection_string", "mongodb://localhost:27017")
                .put("db_name", "test");

        return MongoClient.create(Vertx.vertx(), mongoConfig);
    }

    private JWTAuthOptions getJWTConfig() {
        return new JWTAuthOptions()
                .setKeyStore(new KeyStoreOptions()
                        .setPath("keystore.jceks")
                        .setPassword("password"));
    }

    private JWTAuth getJwtProvider() {
        return  JWTAuth.create(Vertx.vertx(), getJWTConfig());
    }

    private JWTAuthHandlerImpl getJwtHandler() {
        return new JWTAuthHandlerImpl(getJwtProvider(), null);
    }

}
