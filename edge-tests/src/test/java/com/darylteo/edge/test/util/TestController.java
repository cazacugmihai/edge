package com.darylteo.edge.test.util;

import org.edgeframework.controller.Controller;
import org.edgeframework.controller.Result;
import org.edgeframework.promises.Promise;
import org.edgeframework.promises.PromiseHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public class TestController extends Controller {

  public static Result testOkResult() {
    return ok("Hello World");
  }

  public static Result testRenderResult() {
    return render("basic-test", new JsonObject().putString("echo", "Hello World"));
  }

  public static Result testJsonResult() {
    return json(new JsonObject().putString("echo", "Hello World"));
  }

  public static Result testAsyncResult() {
    Promise<Result> promise = Promise.defer(new Handler<Promise<String>>() {
      @Override
      public void handle(Promise<String> promise) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        promise.fulfill("Hello World");
      }
    }).then(
        new PromiseHandler<String, Result>() {
          @Override
          public Result handle(String value) {
            return ok(value);
          }
        }

        );

    return async(promise);
  }

  public static Result TestRouteParams(String echoString) {
    return ok(echoString);
  }
}