package com.scispike.ws;

import java.util.Hashtable;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.scispike.callback.Callback;
import com.scispike.callback.EventEmitter;
import com.scispike.conversation.Agent;
import com.scispike.conversation.AuthFunction;
import com.scispike.conversation.Socket;

public class WsSocket implements Socket {

  static final Map<String, ReconnectingSocket> globalSockets = new Hashtable<String, ReconnectingSocket>();
  
  ReconnectingSocket socket;

  final String urlPrefix;
  final AuthFunction authFunction;
  final Callback<String, String> callback;
  final EventEmitter<String> eventEmitter;

  public WsSocket(final String urlPrefix, final AuthFunction authFunction,
      final Callback<String, String> callback) {
    this.urlPrefix = urlPrefix;
    this.authFunction = authFunction;
    this.callback = callback;
    socket = getReconnectingSocket();
    eventEmitter = new EventEmitter<String>();
    socket.connect(eventEmitter);
  }


  private synchronized ReconnectingSocket getReconnectingSocket() {
    ReconnectingSocket socket = globalSockets.get(urlPrefix);
    if(socket==null){
      socket =  new ReconnectingSocket(urlPrefix, authFunction);
      globalSockets.put(urlPrefix, socket);
    }
    return socket;
  }



  /* (non-Javadoc)
   * @see com.scispike.ws.Socket#isConnected()
   */
  @Override
  public boolean isConnected() {
    if(socket != null){
      return socket.isConnected();
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see com.scispike.ws.Socket#send(java.lang.String, com.scispike.callback.Callback)
   */
  @Override
  public void publish(String topic, JSONObject msg, Callback<String, String> cb) {

    try {
      msg.put("event",topic );
    } catch (JSONException e) {
      e.printStackTrace();
    }
    if (isConnected()) {
      socket.send(msg.toString(), cb);
    } else if (cb != null) {
      cb.call("Not connected");
    }
  }

  /* (non-Javadoc)
   * @see com.scispike.ws.Socket#disconnect()
   */
  @Override
  public void disconnect() {
    if(isConnected()){
      socket.disconnect(eventEmitter);
      socket=null;
    } else {
      eventEmitter.emit("socket::disconnected");
      eventEmitter.removeAllListeners();
    }
  }

  /* (non-Javadoc)
   * @see com.scispike.ws.Socket#getConnectEmitter()
   */
  @Override
  public EventEmitter<String> getConnectEmitter() {
    return eventEmitter;
  }




  @Override
  public Agent buildAgent(String agent, JSONObject agentData) {
    // TODO Auto-generated method stub
    return new WsAgent(agent, this, agentData);
  }


  @Override
  public Agent buildAgent(String agent, String id) {
    return new WsAgent(agent, this, id);
  }

  @Override
  public void subscribe(String topic, Callback<String, String> cb) {
    throw new UnsupportedOperationException();
  }
}
