package com.scispike.ws;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.java_websocket.WebSocket.READYSTATE;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.scispike.callback.Callback;
import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;

public class Socket {

  private static final int bo_min = 100;
  public static int hb_interval = 30000;
  public static int bo_max = 10 * 1000;

  final Map<String, Boolean> hb = new Hashtable<String, Boolean>();
  final Map<String, Integer> backOff = new Hashtable<String, Integer>();
  static final Map<String, SockJsClient> globalSockets = new Hashtable<String, SockJsClient>();
  static final Map<String, Boolean> globalConnections = new Hashtable<String, Boolean>();
  static final Map<String, EventEmitter<String>> globalEventEmitters = new Hashtable<String, EventEmitter<String>>();
  static final Map<String, TimerTask> globalTasks = new Hashtable<String, TimerTask>();
  final Timer timer = new Timer(true);

  final String urlPrefix;
  final AuthFunction authFunction;
  final Callback<String, String> callback;
  final EventEmitter<String> globalEmitter;
  private final EventEmitter<String> connectEmitter;

  public Socket(final String urlPrefix, final AuthFunction authFunction,
      final Callback<String, String> callback) {
    this.urlPrefix = urlPrefix;
    this.authFunction = authFunction;
    this.callback = callback;
    this.globalEmitter = getGlobalEventEmitter();
    connectEmitter = new EventEmitter<String>();
  }

  EventEmitter<String> getGlobalEventEmitter() {
    EventEmitter<String> eventEmitter = globalEventEmitters.get(urlPrefix);
    if (eventEmitter == null) {
      eventEmitter = new EventEmitter<String>();
      globalEventEmitters.put(urlPrefix, eventEmitter);
    }
    return eventEmitter;
  }

  void doReconnect() {
    disconnect();
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
    backOff.put(urlPrefix, Math.max(bo_max, backoff * backoff));
    cancelHeartbeat();
    TimerTask t = new TimerTask() {
      @Override
      public void run() {
        doConnect();
      }
    };
    timer.schedule(t, backoff);

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
    timer.schedule(t, hb_interval);

  }

  void doConnect() {
    globalConnections.put(urlPrefix, true);
    authFunction.auth(new Callback<String, String>() {
      @Override
      public void call(String error, String... args) {
        final String token = args[0];
        final String sessionId = args[1];
        final String url = urlPrefix + "/ws/" + sessionId + "/auth/" + token;
        final SockJsClient mySocket = new SockJsClient(createUrl(url)) {

          @Override
          public void onError(Exception arg0) {
            cancelHeartbeat();
            disconnect();
            globalEmitter.emit("error", arg0.getMessage());
          }

          @Override
          public void onClose(int arg0, String arg1, boolean arg2) {
            cancelHeartbeat();
            globalConnections.put(urlPrefix, false);
            globalSockets.remove(urlPrefix);
            retryConnect();
            globalEmitter.emit("error", "disconnected");
            globalEmitter.removeAllListeners(sessionId + ":connected");
            globalEmitter.removeAllListeners("error");
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
                globalEmitter.emit(event, d.toString());
                getConnectEmitter().emit(event, d.toString());
              } else {
                globalEmitter.emit(event);
                getConnectEmitter().emit(event);
              }

            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        };
        globalEmitter.once(sessionId + ":connected", new Event<String>() {

          @Override
          public void onEmit(String... args) {
            globalSockets.put(urlPrefix, mySocket);
            globalEmitter.emit("connect", args);
            if (callback != null) {
              callback.call(null);
            }
          }
        });
        globalEmitter.on("connect", new Event<String>() {
          @Override
          public void onEmit(String... args) {
            getConnectEmitter().emit("connect", args);
          }
        });
        globalEmitter.once("error", new Event<String>() {
          @Override
          public void onEmit(String... data) {
            getConnectEmitter().emit("error", data);
          };
        });
        mySocket.connect();
      }
    });
  }

  public boolean isConnected() {
    SockJsClient sockJsClient = globalSockets.get(urlPrefix);
    return sockJsClient.getReadyState() == READYSTATE.OPEN;
  }

  public void send(String msg, Callback<String, String> cb) {
    SockJsClient sockJsClient = globalSockets.get(urlPrefix);
    if (sockJsClient != null) {
      JSONArray j = new JSONArray();
      j.put(msg);
      sockJsClient.send(j.toString());
      if (cb != null) {
        cb.call(null);
      }
    } else if (cb != null) {
      cb.call("Not connected");
    }
  }

  public EventEmitter<String> connect() {
    Boolean connected = globalConnections.get(urlPrefix);
    if (connected == null || !connected) {
      doConnect();
    } else {
      getConnectEmitter().emit("connect");
      if (callback != null) {
        callback.call(null);
      }
    }
    return getConnectEmitter();
  }

  public void disconnect() {
    SockJsClient socket = globalSockets.get(urlPrefix);
    globalSockets.remove(urlPrefix);
    if (socket != null)
      socket.close();
  }

  public EventEmitter<String> getConnectEmitter() {
    return connectEmitter;
  }
}
