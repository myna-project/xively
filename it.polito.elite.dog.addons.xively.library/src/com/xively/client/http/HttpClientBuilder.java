// Copyright (c) 2003-2013, LogMeIn, Inc. All rights reserved.
// This is part of Xively4J library, it is under the BSD 3-Clause license.
package com.xively.client.http;


import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;

import com.xively.client.AppConfig;

/**
 * Class for creating HttpClient and allowing downstream application to
 * configure HTTP client behaviours such as retry and timeouts, abstracted from
 * underlying implementation.
 * 
 * 
 * @author s0pau
 */
public class HttpClientBuilder
{
	
	
	// timeouts can be override in the xively configuration
	private static final int DEFAULT_CONNECTION_TIMEOUT_IN_MS = 3000;
	private static final int DEFAULT_SOCKET_TIMEOUT_IN_MS = 3000;
	private static final String DEFAULT_TOKEN = "/token";
	
	private HttpRequestRetryHandler retryHandler;
	private CloseableHttpClient httpClient;
	private Integer socketTimeout;
	private Integer connectionTimeout;
	private String tokenEndpoint;
	

	private static HttpClientBuilder instance;
	// crsf token
	private static String token;

	private HttpClientBuilder()
	{
		connectionTimeout = AppConfig.getInstance().getConnectionTimeout();
		connectionTimeout = (connectionTimeout == null) ? DEFAULT_CONNECTION_TIMEOUT_IN_MS : connectionTimeout;
		
		socketTimeout = AppConfig.getInstance().getSocketTimeout();
		socketTimeout = (socketTimeout == null) ? DEFAULT_SOCKET_TIMEOUT_IN_MS : socketTimeout;
		
		tokenEndpoint = AppConfig.getInstance().getTokenEndpoint();
		tokenEndpoint = (tokenEndpoint.isEmpty()) ? DEFAULT_TOKEN : tokenEndpoint;
	}

	public static HttpClientBuilder getInstance()
	{
		if (instance == null)
		{
			instance = new HttpClientBuilder();
		}
		return instance;
	}
	
	public synchronized void loadCSRFToken(){
	
		HttpRequestBase tokenRequest = new HttpGet(tokenEndpoint);
		HttpResponse tokenResponse;
		
		try {
			
			// exec request
			tokenResponse = getInstance().getHttpClient().execute(tokenRequest);
			// extract token
			setCSRFToken(tokenResponse.getFirstHeader("X-CSRF-TOKEN").getValue());			
			
		} catch (ClientProtocolException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
	}
	
	public String getCSRFToken(){
		return token;
	}
	
	public String setCSRFToken(String token){
		return this.token = token;
	}

	/**
	 * @param retryCount
	 *            number of retries to be attempted by the http client
	 */
	public void setRetryCount(int retryCount)
	{
		retryHandler = new DefaultHttpRequestRetryHandler(retryCount, false);
	}

	/**
	 * @param connectionTimeout
	 *            number of milliseconds before timeout when establising
	 *            connection, default is DEFAULT_CONNECTION_TIMEOUT_IN_MS
	 */
	public void setConnectionTimeout(int connectionTimeout)
	{
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * @param socketTimeout
	 *            number of milliseconds before timeout when waiting for packet
	 *            response, default is DEFAULT_SOCKET_TIMEOUT_IN_MS
	 */
	public void setSocketTimeout(int socketTimeout)
	{
		this.socketTimeout = socketTimeout;
	}

	/**
	 * @return an HttpClient with config as specified in this builder
	 */
	HttpClient getHttpClient()
	{
		if (httpClient == null)
		{
			
			RequestConfig requestConfig = RequestConfig.custom()
					// connectionTimeout the time to establish the connection with the remote host			
					.setConnectTimeout(this.connectionTimeout)
					// socketTimeout is the time waiting for data – after the connection was established; maximum time of inactivity between two data packets
					.setSocketTimeout(this.socketTimeout)
					// connectionRequestTimeout is the time to wait for a connection from the connection manager/pool
					.setConnectionRequestTimeout(this.connectionTimeout)
					.build();
			
			// A timeout exception will print an output like this: "Timeout exception.: exception: Read timed out"
			
			CookieStore httpCookieStore = new BasicCookieStore();
			
			httpClient = HttpClients.custom()
					.setDefaultRequestConfig(requestConfig)
			        .setRetryHandler(retryHandler)
			        // set a cookie store to handle jsessionid
			        .setDefaultCookieStore(httpCookieStore)
			        .build();
			
			if (retryHandler == null)
			{
				retryHandler = new DefaultHttpRequestRetryHandler(0, false);
			}

			//httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
			//httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
		}
		return httpClient;
	}
}
