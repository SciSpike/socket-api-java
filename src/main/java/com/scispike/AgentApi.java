package com.scispike;

import org.json.JSONObject;

public class AgentApi {
  private String agent;
  private SocketApi api;
  private EventEmitter eventEmitter;
  private JSONObject agentData;

  public AgentApi(String agent,SocketApi api,JSONObject agentData) {
    this.agent = agent;
    this.api = api;
    this.eventEmitter = api.getEventEmitter();
    this.agentData = agentData;
  }
  public void emit(String event,JSONObject eventData,Callback cb){
    JSONObject msg = new JSONObject();
    msg.put("event", agent+"::"+event+":"+agentData.getString("_id"));

    JSONObject data = new JSONObject();
    data.put("agentData", agentData);
    data.put("data", eventData);
    
    msg.put("data", data);
    
    api.send(msg.toString(),cb);
  }
  public Callback on(String event,Callback cb){
    return eventEmitter.on(agent+"::"+event+":"+agentData.getString("_id"),cb);
  }

}
