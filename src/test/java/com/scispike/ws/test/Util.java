package com.scispike.ws.test;

import java.net.URLDecoder;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import com.scispike.callback.Callback;
import com.scispike.conversation.AuthFunction;
import com.scispike.conversation.Socket;
import com.scispike.ws.WsSocket;

public class Util {

  public static final String HTTP_HOST = "http://localhost:3000";
  
  public static Socket getSocket(){
    return getSocket(null,new AtomicInteger(0));
  }
  public static WsSocket getSocket(Callback<String, String> connected,final AtomicInteger failures) {
    WsSocket s = new WsSocket(Util.HTTP_HOST, Util.getAuth(failures), connected);
    return s;
  }
  public static AuthFunction getAuth(final AtomicInteger failures) {
    return new AuthFunction() {

      @Override
      public void auth(Callback<String, String> cb) {
        HttpClient client = new HttpClient();
        HttpMethod m = new GetMethod(HTTP_HOST);
        if(failures.getAndDecrement() >0){
          System.out.println("failing: "+failures.get());
          cb.call("bad");
          return;
        } 
        try {
          client.executeMethod(m);
          Header c = m.getResponseHeader("set-cookie");
          String v = c.getValue();
          String[] split = v.split(";");
          String sessionId = URLDecoder.decode(split[0].split("=")[1],"UTF-8").replaceAll("s:([^\\.]*).*", "$1");;
          cb.call(null, "token", sessionId);
        } catch (Exception e) {
          e.printStackTrace();
          cb.call(e.toString());
        }
      }
    };
  }
}
