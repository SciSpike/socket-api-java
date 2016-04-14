package com.scispike.mqtt.test;

import java.util.concurrent.atomic.AtomicInteger;

import com.scispike.callback.Callback;
import com.scispike.conversation.AuthFunction;
import com.scispike.conversation.Socket;
import com.scispike.mqtt.MqttSocket;

public class Util {

	public static final String HTTP = "http://localhost:3000";

	public static Socket getSocket() {
		return getSocket(new AtomicInteger(0));
	}

	public static Socket getSocket(final AtomicInteger failures) {
		Socket s = new MqttSocket(Util.HTTP, Util.getAuth(failures));
		return s;
	}

	public static AuthFunction getAuth(final AtomicInteger failures) {
		return new AuthFunction() {

			@Override
			public void auth(Callback<String, String> cb) {
					cb.call(null, "token", "whocares");
			}
		};
	}
}
