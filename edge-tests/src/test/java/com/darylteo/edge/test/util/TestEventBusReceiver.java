package com.darylteo.edge.test.util;

public interface TestEventBusReceiver {
  public void testString(String message);

  public String testReply(String message);
}