package com.scispike;

import org.json.JSONObject;

public class Agent {
  private String agent;
  private Socket socket;
  private EventEmitter<String> eventEmitter;
  private JSONObject agentData;

  public Agent(String agent, Socket socket, JSONObject agentData) {
    this.agent = agent;
    this.socket = socket;
    this.eventEmitter = socket.getConnectEmitter();
    this.agentData = agentData;
  }

  public void emit(String event, JSONObject eventData, Callback<String,String> cb) {
    JSONObject msg = new JSONObject();
    msg.put("event", agent + "::" + event + ":" + agentData.getString("_id"));

    JSONObject data = new JSONObject();
    data.put("agentData", agentData);
    data.put("data", eventData);

    msg.put("data", data);

    socket.send(msg.toString(), cb);
  }

  public Callback<String,String> on(String event, Event<String> cb) {
    return eventEmitter.on(
        agent + ":state:" + event + ":" + agentData.getString("_id"), cb);
  }

}
