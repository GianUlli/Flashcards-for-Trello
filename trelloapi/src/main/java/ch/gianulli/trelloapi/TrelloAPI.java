/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gian Ulli (gian.ulli@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ch.gianulli.trelloapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ch.gianulli.trelloapi.ui.AuthenticationDialogFragment;

/**
 * Base class that maps the Trello API. It uses <a href="https://developer.android
 * .com/training/volley/index.html">Volley</a> for network requests.
 */
public class TrelloAPI {

	private static final String PREF_NAME = "trello_api";

	/**
	 * Used for storing the access token
	 */
	private static final String PREF_KEY_TOKEN = "token";

	/**
	 * Name of metadata field in manifest that contains the application key
	 */
	private static final String META_DATA_APP_KEY = "ch.gianulli.trelloapi.APP_KEY";

	/**
	 * Name of metadata field in manifest that contains the application secret
	 */
	private static final String META_DATA_APP_SECRET = "ch.gianulli.trelloapi.APP_SECRET";

	private static final String BASE_URL = "https://trello.com/1/";

	private Context mContext;

	private String mAppKey;

	private String mAppSecret;

	private SharedPreferences mPreferences;

	private String mToken;

	private RequestQueue mRequestQueue;

	/**
	 * @param context Current context (e.g. activity)
	 * @throws IllegalArgumentException if the application key and secret are not in a {@code
	 *                                  <meta-data>} tag inside the {@code <application>} tag in
	 *                                  the app's manifest.
	 */
	public TrelloAPI(Context context) {
		mContext = context;
		mPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		mRequestQueue = Volley.newRequestQueue(context);

		// Get application key from <meta-data> tag in manifest
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context
					.getPackageName(), PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;

			if (!bundle.containsKey(META_DATA_APP_KEY)) {
				throw new IllegalArgumentException("Application key could not be found. Have you" +
						" put it in your application manifest?");
			}
			if (!bundle.containsKey(META_DATA_APP_SECRET)) {
				throw new IllegalArgumentException("Application secret could not be found. Have " +
						"you put it in your application manifest?");
			}

			mAppKey = bundle.getString(META_DATA_APP_KEY);
			mAppSecret = bundle.getString(META_DATA_APP_SECRET);
		} catch (PackageManager.NameNotFoundException e) {
			throw new IllegalArgumentException("Application key and secret could not be found. " +
					"Have you put them in your application manifest?");
		}
	}

	/**
	 * Opens a dialog that handles the user authorization
	 *
	 * @param fragmentManager Support fragment manager
	 */
	public void requestAuthorization(FragmentManager fragmentManager) {
		Log.d("TrelloAPI", "requestAuthorization");
		AuthenticationDialogFragment fragment = AuthenticationDialogFragment.newInstance();
		fragment.show(fragmentManager, "dialog");
	}

	/**
	 * Attention: this method makes a synchronous network request!
	 *
	 * @return true if stored token is valid, false otherwise
	 */
	public boolean validateToken() throws TrelloNotAccessibleException {
		try {
			makeStringRequest("GET", "members/me", null, true);
		} catch (TrelloNotAuthorizedException e) {
			return false;
		}
		return true;
	}

	/**
	 * Utility method to make Trello API requests
	 *
	 * @param httpMethod       Either GET, POST, PUT or DELETE
	 * @param path             e.g. "actions/[idAction]"
	 * @param queryArgs        query arguments
	 * @param isTokenNecessary is access token necessary?
	 * @return server answer
	 * @throws TrelloNotAccessibleException if Trello API is not accessible
	 * @throws TrelloNotAuthorizedException if token is not valid
	 * @throws MalformedURLException        if path was not correctly formatted
	 */
	protected JSONArray makeJSONArrayRequest(String httpMethod, String path, Map<String, String>
			queryArgs, boolean isTokenNecessary) throws TrelloNotAccessibleException,
			TrelloNotAuthorizedException {
		// Add key and token to arguments
		if (queryArgs == null) {
			queryArgs = new LinkedHashMap<>();
		}
		queryArgs.put("key", getAppKey());
		if (isTokenNecessary) {
			queryArgs.put("token", getToken());
		}

		// Build argument string
		StringBuilder getData = new StringBuilder();
		try {
			for (Map.Entry<String, String> param : queryArgs.entrySet()) {
				if (getData.length() != 0) {
					getData.append('&');
				}
				getData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				getData.append('=');
				getData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			// never happens
		}

		// Check if httpMethod is supported
		int method = -1;
		if (httpMethod.equals("GET")) {
			method = Request.Method.GET;
		} else if (httpMethod.equals("POST")) {
			method = Request.Method.POST;
		} else if (httpMethod.equals("PUT")) {
			method = Request.Method.PUT;
		} else if (httpMethod.equals("DELETE")) {
			method = Request.Method.DELETE;
		} else {
			throw new IllegalArgumentException("HTTP method not supported: " + httpMethod);
		}


		String url = BASE_URL + path + "?" + getData.toString();

		try {
			RequestFuture<JSONArray> future = RequestFuture.newFuture();
			JsonArrayRequest request = new JsonArrayRequest(method, url, null, future, future);
			mRequestQueue.add(request);

			return future.get(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new TrelloNotAccessibleException("Network request was interrupted.");
		} catch (ExecutionException e) {
			VolleyError ve = (VolleyError) e.getCause();
			if (ve instanceof NoConnectionError || ve instanceof NetworkError) {
				throw new TrelloNotAccessibleException("Device is not connected to the internet.");
			} else if (ve instanceof ParseError) {
				throw new TrelloNotAccessibleException("Server answer was not in valid JSON format" +
						".");
			} else if (ve.networkResponse != null) {
				if (ve.networkResponse.statusCode == 401 || ve.networkResponse.statusCode == 400) {
					throw new TrelloNotAuthorizedException("Server returned error 401");
				} else {
					throw new TrelloNotAccessibleException("Server returned error " + ve
							.networkResponse.statusCode + ": " + new String(ve.networkResponse
							.data));
				}
			} else {
				Log.e("Flashcards for Trello", "An unknown exception was thrown.", e);
				e.printStackTrace();
			}
		} catch (TimeoutException e) {
			throw new TrelloNotAccessibleException("Network request timed out");
		}

		return null;
	}

	/**
	 * Utility method to make Trello API requests
	 *
	 * @param httpMethod       Either GET, POST, PUT or DELETE
	 * @param path             e.g. "actions/[idAction]"
	 * @param queryArgs        query arguments
	 * @param isTokenNecessary is access token necessary?
	 * @return server answer
	 * @throws TrelloNotAccessibleException if Trello API is not accessible
	 * @throws TrelloNotAuthorizedException if token is not valid
	 * @throws MalformedURLException        if path was not correctly formatted
	 */
	protected JSONObject makeJSONObjectRequest(String httpMethod, String path, Map<String, String>
			queryArgs, boolean isTokenNecessary) throws TrelloNotAccessibleException,
			TrelloNotAuthorizedException {
		// Add key and token to arguments
		if (queryArgs == null) {
			queryArgs = new LinkedHashMap<>();
		}
		queryArgs.put("key", getAppKey());
		if (isTokenNecessary) {
			queryArgs.put("token", getToken());
		}

		// Build argument string
		StringBuilder getData = new StringBuilder();
		try {
			for (Map.Entry<String, String> param : queryArgs.entrySet()) {
				if (getData.length() != 0) {
					getData.append('&');
				}
				getData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				getData.append('=');
				getData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			// never happens
		}

		// Check if httpMethod is supported
		int method = -1;
		if (httpMethod.equals("GET")) {
			method = Request.Method.GET;
		} else if (httpMethod.equals("POST")) {
			method = Request.Method.POST;
		} else if (httpMethod.equals("PUT")) {
			method = Request.Method.PUT;
		} else if (httpMethod.equals("DELETE")) {
			method = Request.Method.DELETE;
		} else {
			throw new IllegalArgumentException("HTTP method not supported: " + httpMethod);
		}


		String url = BASE_URL + path + "?" + getData.toString();

		try {
			RequestFuture<JSONObject> future = RequestFuture.newFuture();
			JsonObjectRequest request = new JsonObjectRequest(method, url, null, future, future);
			mRequestQueue.add(request);

			return future.get(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new TrelloNotAccessibleException("Network request was interrupted.");
		} catch (ExecutionException e) {
			VolleyError ve = (VolleyError) e.getCause();
			if (ve instanceof NoConnectionError || ve instanceof NetworkError) {
				throw new TrelloNotAccessibleException("Device is not connected to the internet.");
			} else if (ve instanceof ParseError) {
				throw new TrelloNotAccessibleException("Server answer was not in valid JSON format" +
						".");
			} else if (ve.networkResponse != null) {
				if (ve.networkResponse.statusCode == 401) {
					throw new TrelloNotAuthorizedException("Server returned error 401");
				} else {
					throw new TrelloNotAccessibleException("Server returned error " + ve
							.networkResponse.statusCode + ": " + new String(ve.networkResponse
							.data));
				}
			} else {
				Log.e("Flashcards for Trello", "An unknown exception was thrown.", e);
				e.printStackTrace();
			}
		} catch (TimeoutException e) {
			throw new TrelloNotAccessibleException("Network request timed out");
		}

		return null;
	}

	/**
	 * Utility method to make Trello API requests
	 *
	 * @param httpMethod       Either GET, POST, PUT or DELETE
	 * @param path             e.g. "actions/[idAction]"
	 * @param queryArgs        query arguments
	 * @param isTokenNecessary is access token necessary?
	 * @return server answer
	 * @throws TrelloNotAccessibleException if Trello API is not accessible
	 * @throws TrelloNotAuthorizedException if token is not valid
	 * @throws MalformedURLException        if path was not correctly formatted
	 */
	protected String makeStringRequest(String httpMethod, String path, Map<String, String>
			queryArgs, boolean isTokenNecessary) throws TrelloNotAccessibleException,
			TrelloNotAuthorizedException {
		// Add key and token to arguments
		if (queryArgs == null) {
			queryArgs = new LinkedHashMap<>();
		}
		queryArgs.put("key", getAppKey());
		if (isTokenNecessary) {
			queryArgs.put("token", getToken());
		}

		// Build argument string
		StringBuilder getData = new StringBuilder();
		try {
			for (Map.Entry<String, String> param : queryArgs.entrySet()) {
				if (getData.length() != 0) {
					getData.append('&');
				}
				getData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				getData.append('=');
				getData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
		} catch (UnsupportedEncodingException e) {
			// never happens
		}

		// Check if httpMethod is supported
		int method = -1;
		if (httpMethod.equals("GET")) {
			method = Request.Method.GET;
		} else if (httpMethod.equals("POST")) {
			method = Request.Method.POST;
		} else if (httpMethod.equals("PUT")) {
			method = Request.Method.PUT;
		} else if (httpMethod.equals("DELETE")) {
			method = Request.Method.DELETE;
		} else {
			throw new IllegalArgumentException("HTTP method not supported: " + httpMethod);
		}


		String url = BASE_URL + path + "?" + getData.toString();

		try {
			RequestFuture<String> future = RequestFuture.newFuture();
			StringRequest request = new StringRequest(method, url, future, future);
			mRequestQueue.add(request);

			return future.get(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new TrelloNotAccessibleException("Network request was interrupted.");
		} catch (ExecutionException e) {
			VolleyError ve = (VolleyError) e.getCause();
			if (ve != null && ve.networkResponse != null) {
				if (ve.networkResponse.statusCode == 401) {
					throw new TrelloNotAuthorizedException("Server returned error 401");
				} else {
					throw new TrelloNotAccessibleException("Server returned error " + ve
							.networkResponse.statusCode + ": " + new String(ve.networkResponse
							.data));
				}
			}
		} catch (TimeoutException e) {
			throw new TrelloNotAccessibleException("Network request timed out");
		}

		return null;
	}

	/**
	 * @return application key
	 */
	public String getAppKey() {
		return mAppKey;
	}


	/**
	 * @return application secret
	 */
	public String getAppSecret() {
		return mAppSecret;
	}

	/**
	 * @return Access token or null, if no token is stored.
	 */
	public String getToken() {
		if (mToken == null) {
			mToken = mPreferences.getString(PREF_KEY_TOKEN, null);
		}
		return mToken;
	}

	public void setToken(String token) {
		mToken = token;
		mPreferences.edit().putString(PREF_KEY_TOKEN, token).apply();
	}

	public Context getContext() {
		return mContext;
	}

	public void setContext(Context context) {
		mContext = context;
	}
}
