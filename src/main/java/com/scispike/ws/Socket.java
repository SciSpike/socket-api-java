package com.scispike.ws;

import java.util.Hashtable;
import java.util.Map;

import com.scispike.callback.Callback;
import com.scispike.callback.EventEmitter;

public class Socket {

  static final Map<String, ReconnectingSocket> globalSockets = new Hashtable<String, ReconnectingSocket>();
  
  ReconnectingSocket socket;

  final String urlPrefix;
  final AuthFunction authFunction;
  final Callback<String, String> callback;
  final EventEmitter<String> eventEmitter;

  public Socket(final String urlPrefix, final AuthFunction authFunction,
      final Callback<String, String> callback) {
    this.urlPrefix = urlPrefix;
    this.authFunction = authFunction;
    this.callback = callback;
    socket = getReconnectingSocket();
    eventEmitter = new EventEmitter<String>();
  }


  private synchronized ReconnectingSocket getReconnectingSocket() {
    ReconnectingSocket socket = globalSockets.get(urlPrefix);
    if(socket==null){
      socket =  new ReconnectingSocket(urlPrefix, authFunction);
      globalSockets.put(urlPrefix, socket);
    }
    return socket;
  }



  public boolean isConnected() {
    if(socket != null){
      return socket.isConnected();
    } else {
      return false;
    }
  }

  public void send(String msg, Callback<String, String> cb) {
    if (isConnected()) {
      socket.send(msg, cb);
    } else if (cb != null) {
      cb.call("Not connected");
    }
  }

  public EventEmitter<String> connect() {
    socket.connect(eventEmitter);
    return getConnectEmitter();
  }

  public void disconnect() {
    if(isConnected()){
      socket.disconnect(eventEmitter);
      socket=null;
    } else {
      eventEmitter.emit("socket::disconnected");
      eventEmitter.removeAllListeners();
    }
  }

  public EventEmitter<String> getConnectEmitter() {
    return eventEmitter;
  }
}
