package com.scispike;

public abstract class AuthFunction {
  abstract void auth(Callback<String,String> cb);
}