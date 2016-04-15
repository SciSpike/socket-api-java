package com.scispike.mqtt;

import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import com.scispike.callback.Callback;
import com.scispike.callback.EventEmitter;
import com.scispike.conversation.Agent;
import com.scispike.conversation.AuthFunction;
import com.scispike.conversation.Socket;

public class MqttSocket implements Socket {
	static final Map<String, MqttWrapper> globalSockets = new Hashtable<String, MqttWrapper>();

	private EventEmitter<String> emitter = null;
	private String baseURL;
	MqttWrapper socketClient;

	public MqttSocket(String baseURL,final AuthFunction authFunciton) {
		this(baseURL,authFunciton,null);
	}

	public MqttSocket(String baseURL,final AuthFunction authFunciton, SSLContext sslContext) {
	  this.baseURL = processUrl(baseURL);
		socketClient = globalSockets.get(this.baseURL);
		emitter = new EventEmitter<String>();
		if (socketClient == null) {
		  socketClient = new MqttWrapper(this.baseURL,sslContext,authFunciton,emitter);
		  globalSockets.put(baseURL, socketClient);
		}
		socketClient.connect(emitter);
	}

  private String processUrl(String baseURL) {
    try {
      URL uri = new URL(baseURL);
      if(uri.getProtocol().equals("http")){
        return "tcp://"+uri.getHost()+":"+(uri.getPort()>0?uri.getPort():80);
      } else {
        return "ssl://"+uri.getHost()+":"+(uri.getPort()>0?uri.getPort():443);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

	/* (non-Javadoc)
   * @see com.scispike.mqtt.ISocket#isConnected()
   */
	@Override
  public boolean isConnected() {
		return socketClient != null && socketClient.isConnected();
	}

	/* (non-Javadoc)
   * @see com.scispike.mqtt.ISocket#publish(java.lang.String, java.lang.String)
   */
	@Override
  public void publish(String topic,JSONObject data, Callback<String, String> cb) {
	  String string = data.toString();
		System.out.println("publishing "+topic+"->"+string);
		MqttMessage message = new MqttMessage(string.getBytes());
		socketClient.publish(topic, message,cb);
	}

	/* (non-Javadoc)
   * @see com.scispike.mqtt.ISocket#getConnectEmitter()
   */
	@Override
  public EventEmitter<String> getConnectEmitter() {
		return emitter;
	}

	/* (non-Javadoc)
   * @see com.scispike.mqtt.ISocket#disconnect()
   */
	@Override
  synchronized
	public void disconnect() {
		if (socketClient!=null){
			if(socketClient.disconnect(emitter)){
			  globalSockets.remove(baseURL);
			};
			socketClient = null;
		}
	}

	/* (non-Javadoc)
   * @see com.scispike.mqtt.ISocket#subscribe(java.lang.String)
   */
	@Override
  public void subscribe(String topic,Callback<String, String> cb) {
		socketClient.subscribe(topic, cb);
	}

  @Override
  public Agent buildAgent(String agent, JSONObject agentData) {
    return new MqttAgent(agent, this, agentData);
  }

  @Override
  public Agent buildAgent(String agent, String id) {
    return new MqttAgent(agent, this, id);
  }

}
