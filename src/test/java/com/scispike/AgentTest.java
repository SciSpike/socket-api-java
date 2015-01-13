package com.scispike;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class AgentTest {
  @Test
  public void testConnect(){
    final CountDownLatch signal = new CountDownLatch(1);
    Socket socket = Util.getSocket();
    
    EventEmitter<String> connectEmitter = socket.getConnectEmitter();
    final Agent agent = new Agent("test.obj", socket, UUID.randomUUID().toString());
    
    agent.once("running",new Event<JSONObject>() {
      @Override
      void onEmit(JSONObject... data) {
        System.out.println("state running");
        for(JSONObject d : data){
          System.out.println(d);
        }
        Assert.assertEquals(data.length,1);
        Assert.assertNotNull(data[0]);
        signal.countDown();
      }
    });
    agent.once("null",new Event<JSONObject>() {
      @Override
      void onEmit(JSONObject... data) {
        System.out.println("state null");
        JSONObject o = new JSONObject() {
          {
            put("data", new JSONObject());
          }
        };
        
        agent.emit("signal", o, null);
      }
    });
    connectEmitter.once("connect",new Event<String>() {
      @Override
      void onEmit(String... data) {
        agent.init(null);
      }
    });
    socket.connect();
    try {
      signal.await();// wait for connect
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
    
  }

}
