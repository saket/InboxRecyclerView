package me.saket.baseandroidproject.utils;

import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;
import com.squareup.moshi.JsonAdapter;

@MoshiAdapterFactory
public abstract class AutoValueMoshiAdapterFactory implements JsonAdapter.Factory {

  public static JsonAdapter.Factory create() {
    // AutoValueMoshi_AutoValueMoshiAdapterFactory gets generated
    // only when there's atleast one AutoValue class with a static jsonAdapter() method.
    return new AutoValueMoshi_AutoValueMoshiAdapterFactory();
  }
}
