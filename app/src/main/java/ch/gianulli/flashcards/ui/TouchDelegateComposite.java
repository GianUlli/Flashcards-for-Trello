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

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * from http://stackoverflow.com/questions/6799066/how-to-use-multiple-touchdelegate
 */
public class TouchDelegateComposite extends TouchDelegate {

	private final List<TouchDelegate> delegates = new ArrayList<TouchDelegate>();
	private static final Rect emptyRect = new Rect();

	public TouchDelegateComposite(View view) {
		super(emptyRect, view);
	}

	public void addDelegate(TouchDelegate delegate) {
		if (delegate != null) {
			delegates.add(delegate);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean res = false;
		float x = event.getX();
		float y = event.getY();
		for (TouchDelegate delegate : delegates) {
			event.setLocation(x, y);
			res = delegate.onTouchEvent(event) || res;
		}
		return res;
	}

}
