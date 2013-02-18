package org.edgeframework.routing.middleware;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.edgeframework.promises.Promise;
import org.edgeframework.promises.PromiseHandler;
import org.edgeframework.routing.HttpServerRequest;
import org.edgeframework.routing.HttpServerResponse;
import org.edgeframework.routing.RequestHandler;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.multipart.Attribute;
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import org.jboss.netty.handler.codec.http.multipart.DiskAttribute;
import org.jboss.netty.handler.codec.http.multipart.DiskFileUpload;
import org.jboss.netty.handler.codec.http.multipart.FileUpload;
import org.jboss.netty.handler.codec.http.multipart.HttpDataFactory;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import org.vertx.java.core.buffer.Buffer;

public class BodyParser extends RequestHandler {

  static {
    DiskFileUpload.baseDirectory = null;
    DiskFileUpload.deleteOnExitTemporaryFile = true;
    DiskAttribute.baseDirectory = null;
    DiskAttribute.deleteOnExitTemporaryFile = true;
  }

  // Disk if size exceed MINSIZE
  private static final HttpDataFactory factory = new DefaultHttpDataFactory(false);

  @Override
  public void handle(final HttpServerRequest request, final HttpServerResponse response) {
    try {

      if (!hasPostBody(request)) {
        next();
        return;
      }

      parseBody(request)
          .then(new PromiseHandler<Void, Void>() {
            @Override
            public Void handle(Void value) throws Exception {
              BodyParser.this.next();
              return null;
            }
          });

    } catch (Exception e) {
      // TODO: Error handling
      e.printStackTrace();
    }
  }

  private boolean hasPostBody(HttpServerRequest request) {
    /* Ignore methods without request body */
    String method = request.getMethod().toLowerCase();
    if (!(method.equalsIgnoreCase("post") ||
        method.equalsIgnoreCase("patch") || method.equalsIgnoreCase("put"))) {
      return false;
    }

    /* Check ContentType is Url Encoded or Multipart Form-Data */
    String contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE);
    if (contentType == null) {
      return false;
    }

    contentType = contentType.toLowerCase();
    if (!contentType.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED) &&
        !contentType.startsWith(HttpHeaders.Values.MULTIPART_FORM_DATA)) {
      return false;
    }

    return true;
  }

  private Promise<Void> parseBody(final HttpServerRequest request) {
    return request.getRawBody()
        .then(new PromiseHandler<byte[], Void>() {
          @Override
          public Void handle(byte[] data) throws Exception {
            /* Create a dummy Netty Request with the body */
            final org.vertx.java.core.http.HttpServerRequest vertxReq = request.getUnderlyingRequest();
            final HttpRequest nettyReq = new DefaultHttpRequest(HttpVersion.HTTP_1_1, new HttpMethod(vertxReq.method), vertxReq.uri);

            nettyReq.setChunked(false);
            nettyReq.setContent(ChannelBuffers.wrappedBuffer(data));

            for (Map.Entry<String, String> header : vertxReq.headers().entrySet()) {
              nettyReq.addHeader(header.getKey(), header.getValue());
            }

            DecoderParser decoder = new DecoderParser(new HttpPostRequestDecoder(BodyParser.factory, nettyReq));
            request.getData().put("body", decoder.body);

            return null;
          }
        });

  }

  class DecoderParser {
    public Map<String, Object> body = new HashMap<>();
    public Map<String, Object> files = new HashMap<>();

    public DecoderParser(HttpPostRequestDecoder decoder) throws NotEnoughDataDecoderException, IOException {

      for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
        HttpDataType type = data.getHttpDataType();

        if (type == HttpDataType.Attribute) {

          parseAttribute((Attribute) data);

        } else if (type == HttpDataType.FileUpload) {

          parseFile((FileUpload) data);

        }
      }

      this.body = Collections.unmodifiableMap(this.body);
      this.files = Collections.unmodifiableMap(this.files);

    }

    private void parseAttribute(Attribute attr) throws IOException {
      addField(attr.getName(), attr.getValue(), this.body);
    }

    private void parseFile(FileUpload file) throws IOException {
      if (!file.isCompleted() || file.length() == 0) {
        return;
      }

      /* Factory specifies in-memory file store */
      byte[] fileContents = file.get();

      addField(file.getName(), file.getFilename(), this.body);
      addField(file.getName(), fileContents, this.files);
    }

    private void addField(String key, Object value, Map<String, Object> map) {
      Object existing = map.get(key);

      if (existing == null) {
        // New Value
        map.put(key, value);
      } else {
        // Existing Value - create a list if it isn't one already
        if (!(existing instanceof List<?>)) {
          List<Object> list = new LinkedList<>();
          list.add(existing);

          map.put(key, list);
          existing = list;
        }

        ((List<Object>) existing).add(value);
      }
    }
  }

}
