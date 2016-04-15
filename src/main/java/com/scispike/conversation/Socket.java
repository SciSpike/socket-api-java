package com.scispike.conversation;

import org.json.JSONObject;

import com.scispike.callback.Callback;
import com.scispike.callback.EventEmitter;

public interface Socket {

  public abstract boolean isConnected();

  public abstract void publish(String topic, JSONObject data, Callback<String, String> cb);

  public abstract EventEmitter<String> getConnectEmitter();

  public abstract void disconnect();

  public abstract void subscribe(String topic,Callback<String, String> cb);

  public abstract Agent buildAgent(String agent, JSONObject agentData);
  @Deprecated
  public abstract Agent buildAgent(String agent, String id);

}