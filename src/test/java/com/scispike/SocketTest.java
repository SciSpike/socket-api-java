package com.scispike;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;
import org.junit.Test;


public class SocketTest {
  private static final String HTTP_HOST = "http://localhost:3001";

  @Test
  public void TestSocketCreation() {
    final CountDownLatch signal = new CountDownLatch(1);
    Socket s = new Socket(HTTP_HOST, new AuthFunction() {

      @Override
      void auth(Callback<String, String> cb) {
        HttpClient client = new HttpClient();
        HttpMethod m = new GetMethod(HTTP_HOST);
        try {
          int r = client.executeMethod(m);
          Header[] hs= m.getResponseHeaders();
          Header c = m.getResponseHeader("set-cookie");
          String v = c.getValue();
          String[] split = v.split(";");
          String sessionId = URLDecoder.decode(split[0].split("=")[1]).replaceAll("s:([^\\.]*).*", "$1");;
          cb.call(null, "token", sessionId);
        } catch (HttpException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
      }
    }, new Callback<String, String>() {

      @Override
      void call(String error, String... args) {
        Assert.assertNull(error);
      }
    });
    EventEmitter<String> connect = s.connect();
    connect.on("error", new Event<String>() {
      
      @Override
      void onEmit(String... data) {
        signal.countDown();
        throw new RuntimeException(data[0]);
      }
    });
    connect.on("connect", new Event<String>() {

      @Override
      void onEmit(String... data) {
        Assert.assertTrue(true);
        signal.countDown();
      }
    });
    try {
      signal.await();// wait for connect
      Assert.assertEquals(signal.getCount(), 0);
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
  }
}
