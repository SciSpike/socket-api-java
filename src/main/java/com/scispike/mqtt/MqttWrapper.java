package com.scispike.mqtt;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.scispike.callback.Callback;
import com.scispike.callback.EventEmitter;
import com.scispike.conversation.AuthFunction;

public class MqttWrapper {
  private static final int MAX_BACKOFF = 100;

  private static final int MIN_BACKOFF = 10000;

  final Set<EventEmitter<String>> eventEmitters = new LinkedHashSet<EventEmitter<String>>();

  MqttAsyncClient socketClient;

  private SSLContext sslContext;

  private AuthFunction authFunciton;

  private int backoff = MIN_BACKOFF;

  final Timer timer = new Timer(true);
  class PublishHolder {
    String topic;
    MqttMessage m;
    Callback<String, String> cb;
  }
  
  LinkedList<PublishHolder> pubs = new LinkedList<PublishHolder>();

  MqttWrapper(String baseURL, SSLContext sslContext, AuthFunction authFunciton,
      EventEmitter<String> emitter) {
    if (sslContext != null) {
      this.sslContext = sslContext;
    }
    this.authFunciton = authFunciton;
    try {
      socketClient = new MqttAsyncClient(baseURL,
          MqttAsyncClient.generateClientId(), new MemoryPersistence());
    } catch (MqttException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    socketClient.setCallback(new MqttCallback() {
      public void messageArrived(String topic, MqttMessage message)
          throws Exception {
        String msg = new String(message.getPayload());
        System.out.println("got t:" + topic + " m:" + msg);
        emit(topic, msg);
      }

      
      public void deliveryComplete(IMqttDeliveryToken arg0) {
        // System.out.println("message delivered ");
      }

      public void connectionLost(Throwable arg0) {
        arg0.printStackTrace();
        System.out.println("On connection lost " + arg0.getMessage());
        reconnect(false);
      }
    });
    connect(emitter);
    reconnect(true);
  }

  public void connect(EventEmitter<String> emitter) {
    if (emitter != null) {
      eventEmitters.add(emitter);
    }
  }

  private void doConnect(MqttConnectOptions options) {

    if (options == null)
      options = buildOptions();

    try {
      IMqttToken token = socketClient.connect(options);
      token.waitForCompletion();
      System.out.println("Connected ;)");
      emit("socket::connected");
      for(int i = pubs.size();i>0;i--){
        PublishHolder pub= pubs.peekFirst();
        doPublish(pub);
      }
    } catch (MqttException e) {
      System.out.println("Failed to connect because;");
      e.printStackTrace();
      if (e.getReasonCode() == MqttSecurityException.REASON_CODE_NOT_AUTHORIZED
          || e.getReasonCode() == MqttSecurityException.REASON_CODE_SERVER_CONNECT_ERROR) {
        reconnect(false);
      }
    }
  }

  private MqttConnectOptions buildOptions() {
    MqttConnectOptions options = new MqttConnectOptions();
    if (socketClient.getServerURI().matches("/^ssl|wss/"))
      options.setSocketFactory(sslContext.getSocketFactory());
    options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    return options;
  }

  private void reconnect(final boolean first) {
    final MqttConnectOptions options = buildOptions();
    System.out.println("reconnecting");
    final CountDownLatch l = new CountDownLatch(1);
    authFunciton.auth(new Callback<String, String>() {
      @Override
      public void call(String error, String... args) {
        if (error != null || args.length < 2 || args[0] == null) {
          System.err.println(error + " got args:" + args.length);
        } else {
          final String token = args[0];
          // final String sessionId = args[1];
          options.setUserName("Bearer");
          options.setPassword(token.toCharArray());
          System.out.println("going to connect with token:" + token);
        }
        if(first){
          doConnect(options);
          l.countDown();
        } else {
          retryReconnect(options);
        }
      }

    });
    if(first){
      try {
        l.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void retryReconnect(final MqttConnectOptions options) {
    backoff = Math.min(MAX_BACKOFF, backoff * 2);
    TimerTask t = new TimerTask() {
      @Override
      public void run() {
        System.out.println(new Timestamp(new Date().getTime())
            + " MqttWrapper: try reconnect");
        doConnect(options);
      }
    };
    timer.schedule(t, backoff);
  }

  public void subscribe(String topic,Callback<String, String> cb) {
    try {
      socketClient.subscribe(topic, 1,null,wrapCallback(cb));
    } catch (MqttException e) {
      new RuntimeException(e).printStackTrace();
      //throw new RuntimeException(e);
    }
  }

  public boolean isConnected() {
    return socketClient.isConnected();
  }

  public void publish(String topic, MqttMessage message, Callback<String, String> cb) {
    PublishHolder mqttPublish = new PublishHolder();
    mqttPublish.cb = cb;
    mqttPublish.m = message;
    mqttPublish.topic=topic;
    if(socketClient.isConnected()){
      doPublish(mqttPublish);
    } else {
      pubs.push(mqttPublish);
    }
  }

  private void doPublish(PublishHolder pub) {
    try {
      socketClient.publish(pub.topic, pub.m,null,wrapCallback(pub.cb));
    } catch (MqttPersistenceException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } catch (MqttException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private IMqttActionListener wrapCallback(final Callback<String, String> cb) {
    if(cb == null){
      return null;
    }
    return new IMqttActionListener() {
      
      @Override
      public void onSuccess(IMqttToken asyncActionToken) {
        cb.call(null);
      }
      
      @Override
      public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        cb.call(exception.getMessage());
      }
    };
  }

  public boolean disconnect(EventEmitter<String> eventEmitter) {
    eventEmitters.remove(eventEmitter);
    if (eventEmitters.size() == 0) {
      try {
        IMqttToken token = socketClient.disconnect();
        token.waitForCompletion();
        backoff = MIN_BACKOFF;
      } catch (MqttException e) {
        if (e.getReasonCode() != MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED
            || e.getReasonCode() != MqttException.REASON_CODE_CLIENT_DISCONNECTING)// already
                                                                                   // disconnected
          new RuntimeException(e).printStackTrace();
      }
    }
    eventEmitter.emit("socket::disconnected");
    eventEmitter.removeAllListeners();
    return eventEmitters.size() == 0;
  }

  private void emit(String message, String... data) {
    for (EventEmitter<String> e : eventEmitters) {
      e.emit(message, data);
    }
  }

  public void setSSLContext(SSLContext sslContext) {
    this.sslContext = sslContext;
  }

  public void setAuthFunction(AuthFunction authFunciton) {
    this.authFunciton = authFunciton;
  }

}
