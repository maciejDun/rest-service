package com.code.block.rest_service;

import com.code.block.rest_service.service.RestService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class RestServiceFactory {
    public RestService buildRestService() {
        return new RestService(getMongoClient());
    }

    private MongoClient getMongoClient() {
        JsonObject mongoConfig = new JsonObject()
                .put("connection_string", "mongodb://localhost:27017")
                .put("db_name", "test");

        return MongoClient.create(Vertx.vertx(), mongoConfig);
    }
}
