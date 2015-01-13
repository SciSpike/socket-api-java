package com.scispike;

import org.json.JSONObject;

public class Agent {
  private String agent;
  private Socket socket;
  private EventEmitter<String> eventEmitter;
  private JSONObject agentData;

  static JSONObject wrapId(String _id){
    JSONObject wrapped =new JSONObject();
    wrapped.put("_id", _id);
    return wrapped;
  }
  
  public Agent(String agent, Socket socket, String _id){
    this(agent,socket,wrapId(_id));
  }
  
  public Agent(String agent, Socket socket, JSONObject agentData) {
    this.agent = agent;
    this.socket = socket;
    this.eventEmitter = socket.getConnectEmitter();
    this.agentData = agentData;
  }
  
  public void init(Callback<String,String> cb){
    JSONObject msg = new JSONObject();
    msg.put("event", agent + "::init");
    msg.put("data", agentData);
    socket.send(msg.toString(), cb);
  }

  public void emit(String event, JSONObject eventData, Callback<String,String> cb) {
    JSONObject msg = new JSONObject();
    msg.put("event", agent + "::" + event);

    JSONObject data = new JSONObject();
    msg.put("data", data);

    data.put("agentData", agentData);
    if(eventData != null){
      data.put("data", eventData);
    }

    socket.send(msg.toString(), cb);
  }

  public Callback<String,String> on(String event, final Event<JSONObject> cb) {
    return eventEmitter.on(
        agent + ":state:" + event + ":" + agentData.getString("_id"), new Event<String>() {
          
          @Override
          void onEmit(String... data) {
            if(data.length>0 && data[0] != null){
              cb.onEmit(new JSONObject(data[0]));
            } else {
              cb.onEmit();
            }
          }
        });
  }
  public Callback<String,String> once(String event, final Event<JSONObject> cb) {
    return eventEmitter.once(
        agent + ":state:" + event + ":" + agentData.getString("_id"), new Event<String>() {
          
          @Override
          void onEmit(String... data) {
            System.out.println(data[0]);
            if(data.length>0 && data[0] != null && data[0] != "null"){
              cb.onEmit(new JSONObject(data[0]));
            } else {
              cb.onEmit();
            }
          }
        });
  }

}
