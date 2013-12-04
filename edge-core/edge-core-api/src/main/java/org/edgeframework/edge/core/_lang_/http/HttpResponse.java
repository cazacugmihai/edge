package org.edgeframework.edge.core._lang_.http;

public interface HttpResponse {
  public HttpResponse statusCode(int code);

  public HttpResponse header(String key, Object value);

  public HttpResponse header(String key, String value);

  public HttpResponse write(String content);

  public HttpResponse close();
}
