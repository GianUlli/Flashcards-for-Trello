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
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

public class VerticalMarginItemDecoration extends RecyclerView.ItemDecoration {

	private int mHeight;

	/**
	 * @param context
	 * @param height  Height of decorator in dp
	 */
	public VerticalMarginItemDecoration(Context context, int height) {
		// Convert dp to px
		Resources r = context.getResources();
		mHeight = (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height, r
				.getDisplayMetrics()));
	}

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
	                           RecyclerView.State state) {
		outRect.set(0, 0, 0, mHeight);
		if (parent.getChildAdapterPosition(view) == 0) { // Add space on top
			outRect.top = mHeight;
		}
	}
}
