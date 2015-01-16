package com.scispike.test;

import java.io.IOException;
import java.net.URLDecoder;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import com.scispike.callback.Callback;
import com.scispike.ws.AuthFunction;
import com.scispike.ws.Socket;

public class Util {

  public static final String HTTP_HOST = "http://localhost:3000";
  
  public static Socket getSocket(){
    return getSocket(null);
  }
  public static Socket getSocket(Callback<String, String> connected) {
    Socket s = new Socket(Util.HTTP_HOST, Util.getAuth(), connected);
    return s;
  }
  public static AuthFunction getAuth() {
    return new AuthFunction() {

      @Override
      public void auth(Callback<String, String> cb) {
        HttpClient client = new HttpClient();
        HttpMethod m = new GetMethod(HTTP_HOST);
        try {
          client.executeMethod(m);
          Header c = m.getResponseHeader("set-cookie");
          String v = c.getValue();
          String[] split = v.split(";");
          String sessionId = URLDecoder.decode(split[0].split("=")[1],"UTF-8").replaceAll("s:([^\\.]*).*", "$1");;
          cb.call(null, "token", sessionId);
        } catch (HttpException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
      }
    };
  }
}
