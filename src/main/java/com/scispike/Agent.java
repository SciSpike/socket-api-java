package com.scispike;

import org.json.JSONObject;

public class Agent {
  private String agent;
  private Socket socket;
  private EventEmitter eventEmitter;
  private JSONObject agentData;

  public Agent(String agent, Socket socket, JSONObject agentData) {
    this.agent = agent;
    this.socket = socket;
    this.eventEmitter = socket.getEventEmitter();
    this.agentData = agentData;
  }

  public void emit(String event, JSONObject eventData, Callback cb) {
    JSONObject msg = new JSONObject();
    msg.put("event", agent + "::" + event + ":" + agentData.getString("_id"));

    JSONObject data = new JSONObject();
    data.put("agentData", agentData);
    data.put("data", eventData);

    msg.put("data", data);

    socket.send(msg.toString(), cb);
  }

  public Callback on(String event, Callback cb) {
    return eventEmitter.on(
        agent + ":state:" + event + ":" + agentData.getString("_id"), cb);
  }

}
