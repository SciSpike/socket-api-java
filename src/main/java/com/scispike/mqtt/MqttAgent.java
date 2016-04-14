package com.scispike.mqtt;

import org.json.JSONException;
import org.json.JSONObject;

import com.scispike.callback.Callback;
import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;
import com.scispike.conversation.Agent;
import com.scispike.conversation.Socket;

public class MqttAgent implements Agent {
  private String agent;
  private Socket socket;
  private EventEmitter<String> eventEmitter;
  private JSONObject agentData;
  private String _id;

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

  @Deprecated
  protected MqttAgent(String agent, Socket socket, String _id) {
    this(agent, socket, wrapId(_id));
  }

  protected MqttAgent(final String agent, Socket socket, JSONObject agentData) {
    this._id = agentData.optString("_id");
    this.agent = agent;
    this.socket = socket;
    this.eventEmitter = socket.getConnectEmitter();
    this.agentData = agentData;
  }
  @Override
  public void ready() {
    if (socket.isConnected()) {
      System.out.println("INITIALININIT:"+agent);
      init();
    }
    this.eventEmitter.on("socket::connected", new Event<String>() {
      @Override
      public void onEmit(String... data) {
        System.out.println("INITINITINIT:"+agent);
        init();
      }
    });
  }

  /* (non-Javadoc)
   * @see com.scispike.conversation.IAgent#init()
   */
  @Override
  public void init(){
    listen("+",null);
  }
  /* (non-Javadoc)
   * @see com.scispike.conversation.IAgent#init(com.scispike.callback.Callback)
   */
  @Override
  @Deprecated
  public void init(Callback<String, String> cb) {
    listen("+",cb);
  }

  protected void listen(String event,Callback<String, String> cb) {
    socket.subscribe(agent + "/state/" + event + "/" + this._id, cb);
  }

  /* (non-Javadoc)
   * @see com.scispike.conversation.IAgent#emit(java.lang.String, org.json.JSONObject, com.scispike.callback.Callback)
   */
  @Override
  public void emit(String event, JSONObject eventData,
      Callback<String, String> cb) {
    String fullEvent = agent + "/" + event;

    JSONObject data = new JSONObject();
    jsonPut(data, "agentData", agentData);
    if (eventData != null) {
      jsonPut(data, "data", eventData);
    }
    socket.publish(fullEvent, data,cb);
  }

  /* (non-Javadoc)
   * @see com.scispike.conversation.IAgent#on(java.lang.String, com.scispike.callback.Event)
   */
  @Override
  public Callback<String, String> on(String event, final Event<JSONObject> cb) {
    return eventEmitter.on(agent + "/state/" + event + "/" + _id, eventCB(cb));
  }

  /* (non-Javadoc)
   * @see com.scispike.conversation.IAgent#once(java.lang.String, com.scispike.callback.Event)
   */
  @Override
  public Callback<String, String> once(String event, final Event<JSONObject> cb) {
    return eventEmitter
        .once(agent + "/state/" + event + "/" + _id, eventCB(cb));
  }

  private Event<String> eventCB(final Event<JSONObject> cb) {
    return new Event<String>() {

      @Override
      public void onEmit(String... data) {
        if (data.length > 0 && data[0] != null && !"null".equals(data[0])) {
          JSONObject jsonObject = wrapData(data[0]);
          cb.onEmit(jsonObject);
        } else {
          cb.onEmit();
        }
      }

    };
  }

  /* (non-Javadoc)
   * @see com.scispike.conversation.IAgent#disconnect()
   */
  @Override
  public void disconnect() {
    eventEmitter.removeAllListeners();
  }

}
