package com.scispike.ws;

import com.scispike.callback.Callback;

public abstract class AuthFunction {
  public abstract void auth(Callback<String,String> cb);
}