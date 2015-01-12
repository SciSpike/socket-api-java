package com.scispike;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

public class Socket {

  private static final int bo_min = 100;
  public static int hb_interval = 30000;
  public static int bo_max = 10 * 1000;

  public abstract class AuthFunction {
    abstract void auth(Callback cb);
  }

  final Map<String, Boolean> hb = new Hashtable<String, Boolean>();
  final Map<String, Integer> backOff = new Hashtable<String, Integer>();
  static final Map<String, SockJsClient> globalSockets = new Hashtable<String, SockJsClient>();
  static final Map<String, Boolean> globalConnections = new Hashtable<String, Boolean>();
  static final Map<String, EventEmitter> globalEventEmitters = new Hashtable<String, EventEmitter>();
  static final Map<String, TimerTask> globalTasks = new Hashtable<String, TimerTask>();
  final Timer timer = new Timer(true);

  final String urlPrefix;
  final AuthFunction authFunction;
  final Callback callback;
  final EventEmitter globalEmitter;
  private final EventEmitter connectEmitter;

  public EventEmitter getEventEmitter() {
    EventEmitter eventEmitter = globalEventEmitters.get(urlPrefix);
    if (eventEmitter == null) {
      eventEmitter = new EventEmitter();
      globalEventEmitters.put(urlPrefix, eventEmitter);
    }
    return eventEmitter;
  }

  public Socket(final String urlPrefix, final AuthFunction authFunction,
      final Callback callback) {
    this.urlPrefix = urlPrefix;
    this.authFunction = authFunction;
    this.callback = callback;
    this.globalEmitter = getEventEmitter();
    connectEmitter = new EventEmitter();
  }

  public void disconnect() {
    SockJsClient socket = globalSockets.get(urlPrefix);
    globalSockets.remove(urlPrefix);
    if (socket != null)
      socket.close();
  }

  public void doReconnect() {
    disconnect();
    doConnect();
  }

  public URL createUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public void resetBackoff() {
    backOff.put(urlPrefix, bo_min);
  }

  public void cancelHeartbeat() {
    TimerTask t = globalTasks.get(urlPrefix);
    if (t != null) {
      t.cancel();
      timer.purge();
    }
  }

  public void retryConnect() {
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

  public void resetHeartbeat() {
    hb.put(urlPrefix, false);
    cancelHeartbeat();
    TimerTask t = new TimerTask() {
      @Override
      public void run() {
        if (!hb.get(urlPrefix)) {
          doReconnect();
        } else {
          hb.put(urlPrefix, false);
          timer.schedule(this, hb_interval);
        }
      }
    };
    timer.schedule(t, hb_interval);

  }

  public void doConnect() {
    globalConnections.put(urlPrefix, true);
    authFunction.auth(new Callback() {
      @Override
      void done(String... args) {
        final String token = args[0];
        final String sessionId = args[1];
        final String url = "/ws/" + sessionId + "/token/" + token;
        final SockJsClient mySocket = new SockJsClient(createUrl(url)) {

          @Override
          public void onError(Exception arg0) {
            cancelHeartbeat();
            disconnect();
            globalEmitter.emit("error");
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
              globalEmitter.emit(msg.getString("event"), msg.get("data").toString());
              getConnectEmitter().emit(msg.getString("event"),
                  msg.get("data").toString());
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        };
        globalEmitter.once(sessionId + ":connected", new Callback() {

          @Override
          void done(String... args) {
            globalSockets.put(urlPrefix, mySocket);
            globalEmitter.emit("connect", args);
            if (callback != null) {
              callback.done();
            }
          }
        });
        globalEmitter.on(sessionId + ":connect", new Callback() {
          @Override
          void done(String... args) {
            getConnectEmitter().emit("connect", args);
          }
        });
        globalEmitter.once("error", new Callback() {
          @Override
          void done(String... data) {
            getConnectEmitter().emit("error", data);
          };
        });
      }
    });
  }
  
  public void send(String msg,Callback cb){
    SockJsClient sockJsClient = globalSockets.get(urlPrefix);
    if(sockJsClient != null){
      sockJsClient.send(msg);
      cb.done();
    } else {
      cb.done("Not connected");
    }
    
  }

  public void connect() {
    Boolean connected = globalConnections.get(urlPrefix);
    if (connected == null || !connected) {
      doConnect();
    } else if (callback != null) {
      callback.done();
    }
  }

  public EventEmitter getConnectEmitter() {
    return connectEmitter;
  }
}
