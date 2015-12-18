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

package ch.gianulli.trelloapi.ui;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.gianulli.trelloapi.R;
import ch.gianulli.trelloapi.TrelloAPI;

/**
 * DialogFragment that handles the user authentication process with Trello.
 */
public class AuthenticationDialogFragment extends DialogFragment {


	private static final String URL_AUTHORIZATION = "https://trello.com/1/authorize?";

	private static final String URL_TOKEN = "https://trello.com/1/token/approve";

	private TrelloAPI mApi;

	private WebView mWebView;

	private ProgressBar mProgressIndicator;

	public AuthenticationDialogFragment() {
	}

	/**
	 * @return New instance of fragment
	 */
	public static AuthenticationDialogFragment newInstance() {
		AuthenticationDialogFragment result = new AuthenticationDialogFragment();
		return result;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mApi = new TrelloAPI(getActivity());

		setCancelable(false);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
			savedInstanceState) {
		if (getDialog() != null) {
			getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
		}

		View v = inflater.inflate(R.layout.dialog_authentication, container, false);

		mProgressIndicator = (ProgressBar) v.findViewById(R.id.progress_indicator);
		mWebView = (WebView) v.findViewById(R.id.webview);

		// Catch token
		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				setProgressIndicatorVisibility(true);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				setProgressIndicatorVisibility(false);
				if (url.equals(URL_TOKEN)) {
					// Get token and close dialog
					mWebView.loadUrl("javascript:window.HTMLOUT.parseHTML('<html>'+document" +
							".getElementsByTagName('html')[0].innerHTML+'</html>');");
				} else {
					super.onPageFinished(view, url);
				}
			}
		});
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
		mWebView.setVisibility(View.GONE);

		mWebView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP) {
					switch (keyCode) {
						case KeyEvent.KEYCODE_BACK:
							if (mWebView.canGoBack()) {
								mWebView.goBack();
								return true;
							}
					}

				}
				return false;
			}
		});

		// Load first page
		setProgressIndicatorVisibility(true);
		try {
			mWebView.loadUrl(URL_AUTHORIZATION + "key=" + mApi.getAppKey() + "&name=" +
					URLEncoder.encode(getString(R.string.app_name), "UTF-8") +
					"&expiration=never&response_type=token&scope=read,write");
		} catch (UnsupportedEncodingException e) {
			// never happens
		}

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();

		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup
					.LayoutParams.MATCH_PARENT);
		}
	}

	private void setProgressIndicatorVisibility(boolean visible) {
		if (visible) {
			mProgressIndicator.setVisibility(View.VISIBLE);
			mWebView.setVisibility(View.GONE);
		} else {
			mProgressIndicator.setVisibility(View.GONE);
			mWebView.setVisibility(View.VISIBLE);
		}
	}

	/* Dirty hack to get HTML content of WebView */
	private class MyJavaScriptInterface {
		@JavascriptInterface
		public void parseHTML(final String html) {
			Log.d("test", html);
			Pattern p = Pattern.compile("([0-9a-f]{64})");
			Matcher m = p.matcher(html);
			if (m.find()) {
				String token = m.group(0);

				Log.d("test", "Found token: " + token + ", length: " + token.length());

				mApi.setToken(token);

				dismiss();
			} else {
				// TODO: show some error message
			}
		}
	}
}
