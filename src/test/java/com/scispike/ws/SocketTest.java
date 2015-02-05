package com.scispike.ws;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.scispike.callback.Callback;
import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;
import com.scispike.test.Util;


public class SocketTest {
  
  @Test
  public void authFailure(){
    final CountDownLatch signal = new CountDownLatch(1);
    Socket socket = Util.getSocket(null,new AtomicInteger(2));
    
    EventEmitter<String> connectEmitter = socket.getConnectEmitter();
    connectEmitter.on("connect",new Event<String>() {
      @Override
      public void onEmit(String... data) {
        signal.countDown();
      }
    });
    socket.connect();
    try {
      signal.await(10,TimeUnit.SECONDS);// wait for connect
      Assert.assertTrue(socket.isConnected());
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    } finally {
      socket.disconnect();
    }
  }

  @Test
  public void TestSocketCreation() throws InterruptedException {
    final CountDownLatch signal = new CountDownLatch(1);
    final CountDownLatch waitForHeartbeats = new CountDownLatch(1);
    Callback<String, String> connected = new Callback<String, String>() {

      @Override
      public void call(String error, String... args) {
        Assert.assertNull(error);
      }
    };
    final Socket s = Util.getSocket(connected,new AtomicInteger(0));
    s.hb_interval=400;
    EventEmitter<String> connect = s.getConnectEmitter();
    connect.on("error", new Event<String>() {
      
      @Override
      public void onEmit(String... data) {
        //signal.countDown();
        //waitForHeartbeats.countDown();
        System.out.println(data[0]);
      }
    });
    connect.on("connect", new Event<String>() {
      @Override
      public void onEmit(String... data) {
        Assert.assertTrue(true);
        signal.countDown();
        s.resetHeartbeat();
        s.hb.put(s.urlPrefix, true);
      }
    });
    s.connect();
    try {
      signal.await(10,TimeUnit.SECONDS);// wait for connect
      Assert.assertEquals(signal.getCount(), 0);
      Assert.assertEquals(s.isConnected(), true);
      waitForHeartbeats.await(1,TimeUnit.SECONDS);
      Assert.assertTrue("Yeah no errors", true);
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    } finally {
      s.disconnect();
    }
  }


  
}
