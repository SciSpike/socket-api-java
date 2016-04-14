package com.scispike.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;
import com.scispike.mqtt.MqttWrapper;
import com.scispike.conversation.Socket;
import com.scispike.mqtt.test.Util;

public class SocketTest {

    private static final TimeUnit UNIT = TimeUnit.SECONDS;
    Long time = System.currentTimeMillis()*10000;
    private Socket socket;

    @Before
    public void setUpTestSocket() {
      System.out.println("setUpTestSocket()");
        socket = Util.getSocket();
    }

    @After
    public void tearDownTestSocket() {
        if (socket.isConnected()) {
            socket.disconnect();
        }
    }


    @Test
    public void shouldBeAbleToConnect() {
        final CountDownLatch signal = new CountDownLatch(1);

        EventEmitter<String> connectEmitter = socket.getConnectEmitter();
        if (socket.isConnected()) {
          signal.countDown();
        }
        connectEmitter.on("socket::connected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        try {
            signal.await(2, UNIT);
            assertTrue(socket.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void shouldBeAbleToReconnectAfterAuthFail() {
        final CountDownLatch signal = new CountDownLatch(1);
        Socket reconnectiongSocket = Util.getSocket(new AtomicInteger(5));
        EventEmitter<String> connectEmitter = reconnectiongSocket
                .getConnectEmitter();
        if (socket.isConnected()) {
          signal.countDown();
        }
        connectEmitter.on("socket::connected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        try {
            signal.await(5, UNIT);
            assertTrue(reconnectiongSocket.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } finally {
            reconnectiongSocket.disconnect();
        }
    }

    @Test
    public void shouldBeAbleToReconnect() {
        final CountDownLatch signal = new CountDownLatch(1);

        EventEmitter<String> connectEmitter = socket.getConnectEmitter();
        if (socket.isConnected()) {
          signal.countDown();
        }
        connectEmitter.on("socket::connected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        socket.disconnect();
        Socket socket2 = Util.getSocket();

        try {
            signal.await(2, UNIT);
            assertFalse(socket.isConnected());
            assertTrue(socket2.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } finally {
          socket2.disconnect();
      }
    }

    @Test
    public void shouldBeAbleToDisconnetAfterConnect() {
      final CountDownLatch connect = new CountDownLatch(1);
      final CountDownLatch disconnect = new CountDownLatch(1);

        EventEmitter<String> connectEmitter = socket.getConnectEmitter();
        if(socket.isConnected()){
          connect.countDown();
        }
        connectEmitter.on("socket::connected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
              connect.countDown();
            }
        });
        connectEmitter.on("socket::disconnected", new Event<String>() {
            @Override
            public void onEmit(String... data) {
              disconnect.countDown();
            }
        });

        socket.disconnect();

        try {
          connect.await(1, UNIT);
          disconnect.await(1, UNIT);
          assertFalse(socket.isConnected());
          assertEquals(0, connect.getCount());
          assertEquals(0, disconnect.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void shouldBeAbleToPublishAMessage() throws JSONException {
        final CountDownLatch signal = new CountDownLatch(1);

        EventEmitter<String> connectEmitter = socket.getConnectEmitter();
        connectEmitter.on("defaultTopicResponses", new Event<String>() {
            @Override
            public void onEmit(String... data) {
                signal.countDown();
            }
        });

        socket.subscribe("defaultTopicResponses",null);
        socket.publish("defaultTopic", new JSONObject("{\"who\":\"cares\"}"),null);

        try {
            signal.await(2, UNIT);
            assertTrue(socket.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void shouldBeAbleToStayConnectedFor15sec() {
        final CountDownLatch signal = new CountDownLatch(1);

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

        for (int i = 0; i < 15; i++) {
            System.out.println("subscribing " + (time+i));
            try {
              socket.subscribe("test.obj/state/+/"+(time+i),null);
              socket.publish("test.obj/signal",new JSONObject("{\"agentData\":{\"_id\":\""+(time+i)+"\"},\"data\":{}}"),null);
            } catch (Exception e) {
              System.out.println("FAILED BUT THAT Is OK ********************************************************");
              e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            signal.await(2, UNIT);
            assertTrue(socket.isConnected());
            assertEquals(0, signal.getCount());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    @Ignore
    public void shouldBeAbleToStayConnectedWhileTransmitting() throws JSONException {
        for (int i = 0; i < 10; i++) {
            System.out.println("publishing");
            socket.publish("agent::event", new JSONObject("{}"),null);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertTrue(socket.isConnected());
    }

    @Test
    public void shouldHandleMultipleConnections() throws InterruptedException {
        final CountDownLatch connectState = new CountDownLatch(2);
        final CountDownLatch disconnectState1 = new CountDownLatch(1);
        final CountDownLatch disconnectState2 = new CountDownLatch(1);
        Socket socket2 = Util.getSocket();
        EventEmitter<String> connectEmitter1 = socket.getConnectEmitter();
        EventEmitter<String> connectEmitter2 = socket2.getConnectEmitter();

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
        if (socket.isConnected()) {
          cb.onEmit();
          cb.onEmit();
        }
        connectEmitter1.once("socket::connected", cb);
        connectEmitter2.once("socket::connected", cb);
        connectEmitter1.once("socket::disconnected", dcb1);
        connectEmitter2.once("socket::disconnected", dcb2);

        connectState.await(5, UNIT);
        assertEquals(0, connectState.getCount());
        assertTrue(socket.isConnected());

        socket.disconnect();

        disconnectState1.await(2, UNIT);
        assertEquals(0, disconnectState1.getCount());
        assertFalse(socket.isConnected());
        assertTrue(socket2.isConnected());

        socket2.disconnect();
        disconnectState2.await(2, UNIT);
        assertEquals(0, disconnectState2.getCount());
        assertFalse(socket2.isConnected());
        assertFalse(socket.isConnected());
    }
}
