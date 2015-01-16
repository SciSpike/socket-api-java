package com.scispike.ws;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.scispike.callback.Callback;
import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;
import com.scispike.test.Util;


public class SocketTest {

  @Test
  public void TestSocketCreation() {
    final CountDownLatch signal = new CountDownLatch(1);
    Callback<String, String> connected = new Callback<String, String>() {

      @Override
      public void call(String error, String... args) {
        Assert.assertNull(error);
      }
    };
    Socket s = Util.getSocket(connected);
    EventEmitter<String> connect = s.getConnectEmitter();
    connect.on("error", new Event<String>() {
      
      @Override
      public void onEmit(String... data) {
        signal.countDown();
        throw new RuntimeException(data[0]);
      }
    });
    connect.on("connect", new Event<String>() {

      @Override
      public void onEmit(String... data) {
        Assert.assertTrue(true);
        signal.countDown();
      }
    });
    s.connect();
    try {
      signal.await(1,TimeUnit.MINUTES);// wait for connect
      Assert.assertEquals(signal.getCount(), 0);
      Assert.assertEquals(s.isConnected(), true);
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
  }


  
}
