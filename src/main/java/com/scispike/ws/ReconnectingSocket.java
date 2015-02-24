package com.scispike.ws;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.java_websocket.WebSocket.READYSTATE;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.scispike.callback.Callback;
import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;

public class ReconnectingSocket {

  private static final int bo_min = 100;
  public static int HB_INTERVAL = 30000;
  public static int BO_MAX = 10 * 1000;

  final Map<String, Boolean> hb = new Hashtable<String, Boolean>();
  final Map<String, Integer> backOff = new Hashtable<String, Integer>();
  SockJsClient socket;
  boolean isConnecting = false;

  final Map<String, TimerTask> globalTasks = new Hashtable<String, TimerTask>();
  final Timer timer = new Timer(true);

  final String urlPrefix;
  final AuthFunction authFunction;
  final Set<EventEmitter<String>> eventEmitters = new LinkedHashSet<EventEmitter<String>>();

  public ReconnectingSocket(final String urlPrefix,
      final AuthFunction authFunction) {
    this.urlPrefix = urlPrefix;
    this.authFunction = authFunction;
  }

  void doReconnect() {
    doDisconnect();
    doConnect();
  }

  URL createUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  void resetBackoff() {
    backOff.put(urlPrefix, bo_min);
  }

  void cancelHeartbeat() {
    TimerTask t = globalTasks.get(urlPrefix);
    if (t != null) {
      t.cancel();
      timer.purge();
    }
  }

  void retryConnect() {
    int backoff = backOff.get(urlPrefix);
    backOff.put(urlPrefix, Math.min(BO_MAX, backoff * 2));
    cancelHeartbeat();
    TimerTask t = new TimerTask() {
      @Override
      public void run() {
        doConnect();
      }
    };
    timer.schedule(t, backoff);

  }

  void emit(String message, String... data) {
    int i = 0;
    for (EventEmitter<String> e : eventEmitters) {
      i++;
      e.emit(message, data);
    }
  }

  void resetHeartbeat() {
    hb.put(urlPrefix, false);
    cancelHeartbeat();
    TimerTask t = new TimerTask() {
      @Override
      public void run() {
        if (!hb.get(urlPrefix)) {
          doReconnect();
        } else {
          resetHeartbeat();
        }
      }
    };
    globalTasks.put(urlPrefix, t);
    timer.schedule(t, HB_INTERVAL);

  }

  void doConnect() {
    isConnecting = true;
    authFunction.auth(new Callback<String, String>() {
      @Override
      public void call(String error, String... args) {
        if (error != null) {
          System.err.println(error);
          retryConnect();
          return;
        }
        final String token = args[0];
        final String sessionId = args[1];
        final String url = urlPrefix + "/ws/" + sessionId + "/auth/" + token;
        final SockJsClient mySocket = new SockJsClient(createUrl(url)) {

          @Override
          public void onError(Exception arg0) {
            cancelHeartbeat();
            doDisconnect();
            emit("error", arg0.getMessage());
          }

          @Override
          public void onClose(int arg0, String arg1, boolean arg2) {
            cancelHeartbeat();
            isConnecting = false;

            retryConnect();
            emit("error", "disconnected");
            emit("socket::disconnected");
          }

          @Override
          void onJOpen() {
            resetBackoff();
            resetHeartbeat();
          }

          @Override
          void onJClose() {
            resetBackoff();
            cancelHeartbeat();
          }

          @Override
          void onHeartbeat() {
            hb.put(urlPrefix, true);
          }

          @Override
          void onData(Object data) {
            JSONObject msg = (JSONObject) data;
            try {
              Object d = msg.has("data") ? msg.get("data") : null;
              String event = msg.getString("event");
              if (d != null) {
                emit(event, d.toString());
              } else {
                emit(event);
              }
              if (event.equals(sessionId + ":connected")) {
                socket = this;
                emit("socket::connected");
              }

            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        };
        mySocket.connect();
      }
    });
  }

  public boolean isConnected() {
    return socket != null && socket.getReadyState() == READYSTATE.OPEN;
  }

  public void send(String msg, Callback<String, String> cb) {
    if (socket != null) {
      JSONArray j = new JSONArray();
      j.put(msg);
      socket.send(j.toString());
      if (cb != null) {
        cb.call(null);
      }
    } else if (cb != null) {
      cb.call("Not connected");
    }
  }

  public void connect(EventEmitter<String> eventEmitter) {
    eventEmitters.add(eventEmitter);
    if (!isConnecting) {
      resetBackoff();
      doConnect();
    } else if (isConnected()) {
      eventEmitter.emit("socket::connected");
    }
  }

  private void doDisconnect() {
    resetBackoff();
    try {
      socket.closeBlocking();
      socket = null;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  protected void disconnect(final EventEmitter<String> eventEmitter,
      final Event<String> done) {
    if (eventEmitters.size() ==1 && socket != null) {
      eventEmitter.once("socket::disconnected", new Event<String>() {

        @Override
        public void onEmit(String... data) {
          if (done != null) {
            done.onEmit(data);
          }
          eventEmitters.remove(eventEmitter);
          eventEmitter.removeAllListeners();
        }
      });
      doDisconnect();
    } else {
      eventEmitters.remove(eventEmitter);
      eventEmitter.emit("socket::disconnected");
      eventEmitter.removeAllListeners();
    }
  }

  public void disconnect(EventEmitter<String> eventEmitter) {
    disconnect(eventEmitter, null);
  }

  public void disconnectAll(Event<String> done) {
    if(eventEmitters.size()>0){
      for (EventEmitter<String> eventEmitter : eventEmitters) {
        disconnect(eventEmitter, done);
      }
    } else {
       done.onEmit();
    }
  }

}
