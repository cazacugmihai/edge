package com.darylteo.edge.test.client;

import org.edgeframework.eventbus.EventBus;
import org.edgeframework.promises.PromiseHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.deploy.impl.VertxLocator;
import org.vertx.java.testframework.TestClientBase;

import com.darylteo.edge.test.util.TestEventBusReceiver;
import com.darylteo.edge.test.util.TestEventBusSender;

public class EventBusTestClient extends TestClientBase {
  private static final String REMOTE_NAMESPACE = "remote.namespace";
  private Vertx vertx = VertxLocator.vertx;

  @Override
  public void start() {
    super.start();

    tu.appReady();
  }

  @Override
  public void stop() {
    super.stop();
  }

  public void testSend() {

    VertxLocator.vertx.eventBus().registerHandler(REMOTE_NAMESPACE + ".testString", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        tu.azzert(message.body.getString("message").equals("Hello World"), "Message sent on event bus is not correct");
        tu.testComplete();
      }
    });

    TestEventBusSender server = EventBus.registerSender(REMOTE_NAMESPACE, TestEventBusSender.class);

    server.testString("Hello World");
  }

  public void testReceive() {
    EventBus.registerReceiver(REMOTE_NAMESPACE, new Receiver() {
      @Override
      public void testString(String message) {
        tu.azzert(message.equals("Hello World"));
        tu.testComplete();
      }
    }, TestEventBusReceiver.class);

    VertxLocator.vertx.eventBus().send(REMOTE_NAMESPACE + ".testString", new JsonObject().putString("message", "Hello World"));
  }

  public void testBothEnds() {
    TestEventBusSender server = EventBus.registerSender(REMOTE_NAMESPACE, TestEventBusSender.class);

    EventBus.registerReceiver(REMOTE_NAMESPACE, new Receiver() {
      @Override
      public void testString(String message) {
        tu.azzert(message.equals("Hello World"));
        tu.testComplete();
      }
    }, TestEventBusReceiver.class);

    server.testString("Hello World");
  }

  public void testReply() {
    TestEventBusSender server = EventBus.registerSender(REMOTE_NAMESPACE, TestEventBusSender.class);

    EventBus.registerReceiver(REMOTE_NAMESPACE, new Receiver() {
      @Override
      public String testReply(String message) {
        return message.toUpperCase();
      }
    }, TestEventBusReceiver.class);

    server
        .testReply("Hello World")
        .then(new PromiseHandler<String, Void>() {

          @Override
          public Void handle(String value) {
            tu.azzert(value.equals("HELLO WORLD"), "Did not reply message properly");
            tu.testComplete();

            return null;
          }
        });

  }

  public void testMultipleParameters() {
    TestEventBusSender server = EventBus.registerSender(REMOTE_NAMESPACE, TestEventBusSender.class);

    EventBus.registerReceiver(REMOTE_NAMESPACE, new Receiver() {
      @Override
      public String testMultipleParameters(String message, Number value) {
        String result = "";

        for (int i = 0; i < value.intValue(); i++) {
          result += message;
        }

        return result;
      }
    }, TestEventBusReceiver.class);

    server
        .testMultipleParameters("Hello World", 2)
        .then(new PromiseHandler<String, Void>() {

          @Override
          public Void handle(String value) {
            tu.azzert(value.equals("Hello WorldHello World"), "Did not handle multiple parameters properly");
            tu.testComplete();

            return null;
          }
        });

  }

  public void testParameterTypes1() {
    TestEventBusSender server = EventBus.registerSender(REMOTE_NAMESPACE, TestEventBusSender.class);

    EventBus.registerReceiver(REMOTE_NAMESPACE, new Receiver() {
      @Override
      public void testParameterTypes1(byte n1, short n2, int n3, long n4) {
        tu.azzert(n1 == 1);
        tu.azzert(n2 == 2);
        tu.azzert(n3 == 3);
        tu.azzert(n4 == 4l);
        tu.testComplete();
      }
    }, TestEventBusReceiver.class);

    server.testParameterTypes1((byte) 1, (short) 2, 3, 4l);
  }

  public void testParameterTypes2() {
    TestEventBusSender server = EventBus.registerSender(REMOTE_NAMESPACE, TestEventBusSender.class);

    EventBus.registerReceiver(REMOTE_NAMESPACE, new Receiver() {
      @Override
      public void testParameterTypes2(Byte n1, Short n2, Integer n3, Long n4) {
        tu.azzert(n1 == 1);
        tu.azzert(n2 == 2);
        tu.azzert(n3 == 3);
        tu.azzert(n4 == 4l);
        tu.testComplete();
      }
    }, TestEventBusReceiver.class);

    server.testParameterTypes2((byte) 1, (short) 2, 3, 4l);
  }

  public void testParameterTypes3() {
    TestEventBusSender server = EventBus.registerSender(REMOTE_NAMESPACE, TestEventBusSender.class);

    EventBus.registerReceiver(REMOTE_NAMESPACE, new Receiver() {
      @Override
      public void testParameterTypes3(float f1, double f2) {
        tu.azzert(f1 == 1.5f);
        tu.azzert(f2 == 2.5);
        tu.testComplete();
      }
    }, TestEventBusReceiver.class);

    server.testParameterTypes3(1.5f, 2.5);
  }

  public void testParameterTypes4() {
    TestEventBusSender server = EventBus.registerSender(REMOTE_NAMESPACE, TestEventBusSender.class);

    EventBus.registerReceiver(REMOTE_NAMESPACE, new Receiver() {
      @Override
      public void testParameterTypes4(Float f1, Double f2) {
        tu.azzert(f1 == 1.5f);
        tu.azzert(f2 == 2.5);
        tu.testComplete();
      }
    }, TestEventBusReceiver.class);

    server.testParameterTypes4(1.5f, 2.5);
  }
}

class Receiver implements TestEventBusReceiver {

  @Override
  public void testString(String message) {
  }

  @Override
  public String testReply(String message) {
    return null;
  }

  @Override
  public String testMultipleParameters(String message, Number integer) {
    return null;
  }

  @Override
  public void testParameterTypes1(byte n1, short n2, int n3, long n4) {
  }

  @Override
  public void testParameterTypes2(Byte n1, Short n2, Integer n3, Long n4) {
  }

  @Override
  public void testParameterTypes3(float n1, double n2) {
  }

  @Override
  public void testParameterTypes4(Float n1, Double n2) {
  }
}