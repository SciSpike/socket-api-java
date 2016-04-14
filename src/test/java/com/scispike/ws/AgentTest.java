package com.scispike.ws;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.scispike.callback.Callback;
import com.scispike.callback.Event;
import com.scispike.callback.EventEmitter;
import com.scispike.conversation.Agent;
import com.scispike.conversation.Socket;
import com.scispike.ws.test.Util;
import com.scispike.ws.WsAgent;

/*
 * Prior to running this tests you should:
 * sudo npm install -g conversation 
 * conversation create test
 * cd test
 * npm run gen-src
 * npm start
 */
public class AgentTest {
  
  @Test
  public void testConnect(){
    final CountDownLatch signal = new CountDownLatch(1);
    Socket socket = Util.getSocket();
    EventEmitter<String> connectEmitter = socket.getConnectEmitter();
    final Agent agent = new WsAgent("test.obj", socket, UUID.randomUUID().toString());
    agent.on("error", new Event<JSONObject>() {
      
      @Override
      public void onEmit(JSONObject... data) {
        Assert.fail(data[0].toString());
      }
    });
    agent.once("running",new Event<JSONObject>() {
      @Override
      public void onEmit(JSONObject... data) {
        System.out.println("state running");
        for(JSONObject d : data){
          System.out.println(d);
        }
        Assert.assertEquals(data.length,1);
        Assert.assertNotNull(data[0]);
        agent.on("running",new Event<JSONObject>() {
          @Override
          public void onEmit(JSONObject... data) {
            signal.countDown();
          }
        });
        agent.init(null);
      }
    });
    agent.once("null",new Event<JSONObject>() {
      @Override
      public void onEmit(JSONObject... data) {
        System.out.println("state null");
        JSONObject o;
        try {
          o = new JSONObject() {
            {
              put("data", new JSONObject());
            }
          };
          agent.emit("signal", o, new Callback<String, String>() {
            
            @Override
            public void call(String error, String... args) {
              Assert.assertNull(error);
            }
          });
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
        
      }
    });
    agent.ready();
    try {
      signal.await();// wait for connect
      Assert.assertEquals("should have gotten to running",0, signal.getCount());
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    } finally {
      socket.disconnect();
    }
    
  }

}
