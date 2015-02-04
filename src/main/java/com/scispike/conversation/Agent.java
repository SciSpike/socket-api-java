package com.scispike.conversation;

import org.json.JSONException;
import org.json.JSONObject;

import com.scispike.callback.Callback;
import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;
import com.scispike.ws.Socket;

public class Agent {
  private String agent;
  private Socket socket;
  private EventEmitter<String> eventEmitter;
  private JSONObject agentData;

  static void jsonPut(JSONObject json, String key, String value) {
    try {
      json.put(key, value);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  static void jsonPut(JSONObject json, String key, JSONObject value) {
    try {
      json.put(key, value);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  static Object jsonGet(JSONObject json, String key) {
    try {
      return json.get(key);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  static JSONObject wrapData(String data) {
    try {
      return new JSONObject(data);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  static JSONObject wrapId(String _id) {
    JSONObject wrapped = new JSONObject();
    jsonPut(wrapped, "_id", _id);
    return wrapped;
  }

  public Agent(String agent, Socket socket, String _id) {
    this(agent, socket, wrapId(_id));
  }

  public Agent(String agent, Socket socket, JSONObject agentData) {
    this.agent = agent;
    this.socket = socket;
    this.eventEmitter = socket.getConnectEmitter();
    this.agentData = agentData;
  }

  public void init(Callback<String, String> cb) {
    JSONObject msg = new JSONObject();
    jsonPut(msg, "event", agent + "::init");
    jsonPut(msg, "data", agentData);
    socket.send(msg.toString(), cb);
  }

  public void emit(String event, JSONObject eventData,
      Callback<String, String> cb) {
    JSONObject msg = new JSONObject();
    jsonPut(msg, "event", agent + "::" + event);

    JSONObject data = new JSONObject();
    jsonPut(msg, "data", data);

    jsonPut(data, "agentData", agentData);
    if (eventData != null) {
      jsonPut(data, "data", eventData);
    }

    socket.send(msg.toString(), cb);
  }

  public Callback<String, String> on(String event, final Event<JSONObject> cb) {
    return eventEmitter.on(
        agent + ":state:" + event + ":" + jsonGet(agentData, "_id"),
        eventCB(cb));
  }

  public Callback<String, String> once(String event, final Event<JSONObject> cb) {
    return eventEmitter.once(
        agent + ":state:" + event + ":" + jsonGet(agentData, "_id"),
        eventCB(cb));
  }

  private Event<String> eventCB(final Event<JSONObject> cb) {
    return new Event<String>() {

      @Override
      public void onEmit(String... data) {
        if (data.length > 0 && data[0] != null && data[0] != "null") {
          JSONObject jsonObject = wrapData(data[0]);
          cb.onEmit(jsonObject);
        } else {
          cb.onEmit();
        }
      }

    };
  }

}
