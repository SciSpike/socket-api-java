package com.scispike.ws;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class WebSocketTransport extends WebSocketClient {

  public WebSocketTransport(URI serverURI) {
    super(serverURI, new Draft_17());
  }

  public static int CONNECTING = 0;
  public static int OPEN = 1;
  public static int CLOSING = 2;
  public static int CLOSED = 3;

  private int readyState = WebSocketTransport.CONNECTING;

  @Override
  public void onMessage(String data) {
    JSONArray payload;
    String type = data.substring(0, 1);
    String sData = data.length() > 1 ? data.substring(1) : null;
    try {
      switch (EventType.getFromName(type)) {
      case OPEN:
        this._dispatchOpen();
        break;
      case ACCEPT:
        payload = sData != null ? new JSONArray(sData) : new JSONArray();
        int i = 0;
        while (i < payload.length()) {
          Object o = payload.get(i++);
          JSONObject d = new JSONObject((String)o);
          this._dispatchMessage(d);
        }
        break;
      case MESSAGE:
        this._dispatchMessage(sData != null ? new JSONObject(sData) : null);
        break;
      case CLOSE:
        payload = sData != null ? new JSONArray(sData) : new JSONArray();
        this._didClose(payload.get(0), payload.get(1));
        break;
      case HEARTBEAT:
        this._dispatchHeartbeat();
        break;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void _dispatchHeartbeat() {
    if (readyState == WebSocketTransport.OPEN){
      onHeartbeat();
    }
  }


  private void _didClose(Object object, Object object2) {
    if(readyState == WebSocketTransport.CLOSED){
      throw new UnsupportedOperationException("already closed");
    } else {
      onJClose();
    }

  }


  private void _dispatchMessage(Object data) {
    if (readyState == WebSocketTransport.OPEN){
      onData(data);
    }

  }


  private void _dispatchOpen() {
    if (readyState == WebSocketTransport.CONNECTING) {
      readyState = WebSocketTransport.OPEN;
      onJOpen();
    } else {
      // The server might have been restarted, and lost track of our
      // connection.
      _didClose(1006, "Server lost session");
    }

  }

  @Override
  public void onOpen(ServerHandshake arg0) {
  }
  
  abstract void onHeartbeat();
  abstract void onData(Object data);
  abstract void onJOpen();
  abstract void onJClose();

}
