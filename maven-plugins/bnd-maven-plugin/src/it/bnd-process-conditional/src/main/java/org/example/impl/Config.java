package org.example.impl;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class Config {
	public static void main(String[] args) {
		Cache<String, String> myCache = CacheBuilder.newBuilder().maximumSize(100)
				.expireAfterWrite(30, TimeUnit.SECONDS).build();
	}
}
