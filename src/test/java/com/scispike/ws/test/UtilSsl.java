package com.scispike.ws.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import com.scispike.callback.Callback;
import com.scispike.conversation.AuthFunction;
import com.scispike.conversation.Socket;
import com.scispike.ws.WsSocket;
import com.scispike.ws.WebSocketTransport;

public class UtilSsl {
  

  public static SSLContext sslContext = null;

  public static final String HTTP_HOST = "https://localhost:3000";

  public static Socket getSocket() {
    return getSocket(null, new AtomicInteger(0));
  }

  public static Socket getSocket(Callback<String, String> connected,
      final AtomicInteger failures) {
    WebSocketTransport.setSslContext(getSSLContext());
    Socket s = new WsSocket(UtilSsl.HTTP_HOST, UtilSsl.getAuth(failures), connected);
    return s;
  }

  public static AuthFunction getAuth(final AtomicInteger failures) {
    return new AuthFunction() {

      @Override
      public void auth(Callback<String, String> cb) {
        HttpClient client = new HttpClient();
        

         ProtocolSocketFactory secureProtocolSocketFactory = new SecureProtocolSocketFactory() {

          @Override
          public java.net.Socket createSocket(String host, int port,
              InetAddress localAddress, int localPort) throws IOException,
              UnknownHostException {
            throw new RuntimeException("here");
          }

          @Override
          public java.net.Socket createSocket(String host, int port,
              InetAddress localAddress, int localPort,
              HttpConnectionParams params) throws IOException,
              UnknownHostException, ConnectTimeoutException {
            return getSSLContext().getSocketFactory().createSocket(host, port, localAddress, localPort);
          }

          @Override
          public java.net.Socket createSocket(String host, int port)
              throws IOException, UnknownHostException {
            throw new RuntimeException("here");
          }

          @Override
          public java.net.Socket createSocket(java.net.Socket socket,
              String host, int port, boolean autoClose) throws IOException,
              UnknownHostException {
            throw new RuntimeException("here");
          }
        };
        Protocol.registerProtocol("https", new Protocol("https",
            secureProtocolSocketFactory , 3000));
        HttpMethod m = new GetMethod(HTTP_HOST);
        if (failures.getAndDecrement() > 0) {
          System.out.println("failing: " + failures.get());
          cb.call("bad");
          return;
        }
        try {
          client.executeMethod(m);
          Header c = m.getResponseHeader("set-cookie");
          String v = c.getValue();
          String[] split = v.split(";");
          String sessionId = URLDecoder.decode(split[0].split("=")[1], "UTF-8")
              .replaceAll("s:([^\\.]*).*", "$1");
          ;
          cb.call(null, "token", sessionId);
        } catch (Exception e) {
          e.printStackTrace();
          cb.call(e.toString());
        }
      }
    };
  }

  public static SSLContext getSSLContext() {
    if(sslContext != null){
      return sslContext;
    } else {
      try {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = UtilSsl.class.getResourceAsStream("/server-crt.pem");
        Certificate ca;
        try {
          ca = cf.generateCertificate(caInput);
          System.out.println("Loaded Certificate: " + ((X509Certificate) ca).getSubjectDN());
        } finally {
          caInput.close();
        }
  
        // Create a KeyStore containing our cert
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);
  
        // Create a TrustManager that trusts the certs in our store
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);
        
        KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
        kmf.init( keyStore,null);
        
        sslContext = SSLContext.getInstance( "TLS" );
        sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
        return sslContext;
  
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

  }
}
