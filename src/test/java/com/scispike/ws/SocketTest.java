package com.scispike.ws;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.scispike.callback.Callback;
import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;
import com.scispike.conversation.Socket;
import com.scispike.ws.test.Util;

public class SocketTest {
  

  private static final TimeUnit UNIT = TimeUnit.SECONDS;

  @Test
  public void authFailure() {
    final CountDownLatch signal = new CountDownLatch(1);
    Socket socket = Util.getSocket(null, new AtomicInteger(2));

    EventEmitter<String> connectEmitter = socket.getConnectEmitter();
    if(socket.isConnected()){
      signal.countDown();
    }
    connectEmitter.on("socket::connected", new Event<String>() {
      @Override
      public void onEmit(String... data) {
        signal.countDown();
      }
    });
    try {
      signal.await(10, UNIT);// wait for connect
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
    final WsSocket s = Util.getSocket(connected, new AtomicInteger(0));
    ReconnectingSocket.HB_INTERVAL = 400;
    EventEmitter<String> connect = s.getConnectEmitter();
    connect.on("error", new Event<String>() {

      @Override
      public void onEmit(String... data) {
        // signal.countDown();
        // waitForHeartbeats.countDown();
        System.out.println(data[0]);
      }
    });
    if(s.isConnected()){
      signal.countDown();
    }
    connect.on("socket::connected", new Event<String>() {
      @Override
      public void onEmit(String... data) {
        Assert.assertTrue(true);
        signal.countDown();
        s.socket.resetHeartbeat();
        s.socket.hb.put(s.urlPrefix, true);
      }
    });
    try {
      signal.await(10, UNIT);// wait for connect
      Assert.assertEquals(signal.getCount(), 0);
      Assert.assertEquals(s.isConnected(), true);
      waitForHeartbeats.await(1, UNIT);
      Assert.assertTrue("Yeah no errors", true);
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    } finally {
      s.disconnect();
    }
  }

  @Test
  public void TestMultipleConnections() throws Exception {
    final CountDownLatch connectState = new CountDownLatch(2);
    final CountDownLatch disconnectState1 = new CountDownLatch(1);
    final CountDownLatch disconnectState2 = new CountDownLatch(1);

    
    final WsSocket s1 = Util.getSocket(null, new AtomicInteger(0));
    final Socket s2 = Util.getSocket(null, new AtomicInteger(0));
    ReconnectingSocket s = s1.socket;

    EventEmitter<String> connectEmitter1 = s1.getConnectEmitter();
    EventEmitter<String> connectEmitter2 = s2.getConnectEmitter();

    Event<String> cb = new Event<String>() {

      @Override
      public void onEmit(String... data) {
        connectState.countDown();
      }
    };
    Event<String> dcb1 = new Event<String>() {
      @Override
      public void onEmit(String... data) {
        disconnectState1.countDown();
      }
    };
    Event<String> dcb2 = new Event<String>() {
      @Override
      public void onEmit(String... data) {
        disconnectState2.countDown();
      }
    };
    if(s1.isConnected()){
      cb.onEmit();
    }
    if(s2.isConnected()){
      cb.onEmit();
    }
    connectEmitter1.once("socket::connected", cb);
    connectEmitter2.once("socket::connected", cb);
    connectEmitter1.once("socket::disconnected", dcb1);
    connectEmitter2.once("socket::disconnected", dcb2);
    
    connectState.await(2, UNIT);
    Assert.assertEquals(0, connectState.getCount());
    Assert.assertEquals(2, s1.socket.eventEmitters.size());
    
    s1.disconnect();
    
    disconnectState1.await(2, UNIT);
    Assert.assertEquals(0,disconnectState1.getCount());
    Assert.assertEquals(1, s.eventEmitters.size());
    Assert.assertFalse(s1.isConnected());
    Assert.assertTrue(s.isConnected());

    s2.disconnect();
    disconnectState2.await(2, UNIT);
    Assert.assertEquals(0,disconnectState2.getCount());
    Assert.assertEquals(0, s.eventEmitters.size());
    Assert.assertFalse(s2.isConnected());
    Assert.assertFalse(s.isConnected());
    
  }

  @Test
  public void TestIsConnected() {
    final CountDownLatch signal = new CountDownLatch(1);
    Callback<String, String> connected = new Callback<String, String>() {
      @Override
      public void call(String error, String... args) {
        Assert.assertNull(error);
      }
    };
    final Socket s = Util.getSocket(connected, new AtomicInteger(0));
    EventEmitter<String> connect = s.getConnectEmitter();
    connect.on("socket::disconnected", new Event<String>() {
      @Override
      public void onEmit(String... data) {
        Assert.assertTrue(true);
        signal.countDown();
      }
    });
    if(s.isConnected()){
      signal.countDown();
    }
    connect.on("socket::connected", new Event<String>() {
      @Override
      public void onEmit(String... data) {
        Assert.assertTrue(true);
        signal.countDown();
      }
    });
    try {
      signal.await(2, UNIT);// wait for connect
      s.disconnect();
      signal.await(2, UNIT);// wait for connect
      Assert.assertEquals(s.isConnected(), false);
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
  }

}
