package com.scispike.conversation;

import org.json.JSONObject;

import com.scispike.callback.Callback;
import com.scispike.callback.Event;

public interface Agent {

  public abstract void init();

  public abstract void init(Callback<String, String> cb);


  public abstract void emit(String event, JSONObject eventData,
      Callback<String, String> cb);

  public abstract Callback<String, String> on(String event, Event<JSONObject> cb);

  public abstract Callback<String, String> once(String event,
      Event<JSONObject> cb);

  public abstract void disconnect();

  void ready();

}