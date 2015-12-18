/*
 *    Copyright 2015 Gian Ulli
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ch.gianulli.flashcards.ui;

import android.content.Context;
import android.util.AttributeSet;

import org.markdownj.MarkdownProcessor;

import us.feras.mdv.MarkdownView;

/**
 * Provides an extension to MarkdownView that allows setting a text color and including support
 * for MathML by using MathJax (see https://www.mathjax.org/ for more information)
 */
public class StyledMarkdownView extends MarkdownView {

	public StyledMarkdownView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public StyledMarkdownView(Context context) {
		super(context);
	}

	/**
	 * Loads the given Markdown text to the view as rich formatted HTML.
	 *
	 * @param txt       input in Markdown format
	 * @param textColor text color in hex format, e.g. "#ffffff"
	 */
	public void loadMarkdownWithColor(String txt, String textColor) {
		MarkdownProcessor m = new MarkdownProcessor();
		String html = "<!DOCTYPE html>" +
				"<html>" +
				"<head>" +
				"<style>html, body { color: " + textColor + "; }" +
				"img, a, p, span, h1, h2, h3, h4, ul { max-width: 100% }</style>" +
				"</head>" +
				"<body>" + m.markdown(txt) + "</body>" +
				"</html>";
		loadDataWithBaseURL("fake://", html, "text/html", "UTF-8", null);
	}

	public void loadMarkdownWithMathML(String txt, String textColor) {
		MarkdownProcessor m = new MarkdownProcessor();
		String html = "<!DOCTYPE html>" +
				"<html>" +
				"<head>" +
				"<script type=\"text/x-mathjax-config\">\n" +
				"  MathJax.Hub.Config({tex2jax: {inlineMath: [['\\\\(','\\\\)']]}});" +
				"\n" +
				"</script>\n" +
				"<script type=\"text/javascript\" src=\"https://cdn.mathjax" +
				".org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML\"></script>" +
				"<style>html, body { color: " + textColor + "; }" +
				"img, a, p, span, h1, h2, h3, h4, ul { max-width: 100% }</style>" +
				"</head>" +
				"<body>" + m.markdown(txt) + "</body>" +
				"</html>";
		loadDataWithBaseURL("fake://", html, "text/html", "UTF-8", null);
	}


}
