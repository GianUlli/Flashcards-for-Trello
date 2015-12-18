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

public class MarginItemDecoration extends RecyclerView.ItemDecoration {

	private int mMargin;

	private GridMetricsProvider mProvider;

	/**
	 * @param context
	 * @param margin   Margin in dp
	 * @param provider
	 */
	public MarginItemDecoration(Context context, int margin, GridMetricsProvider provider) {
		// Convert dp to px
		Resources r = context.getResources();
		mMargin = (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, margin, r
				.getDisplayMetrics()));
		mProvider = provider;
	}

	@Override
	public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
	                           RecyclerView.State state) {
		int n = mProvider.getNumberOfGridColumns();
		int i = parent.getChildAdapterPosition(view);

		outRect.set(0, 0, mMargin, mMargin);
		if (i < n) { // child is in top row
			outRect.top = mMargin;
		}
		if (i % n == 0) { // child is in left column
			outRect.left = mMargin;
		}
	}

	public interface GridMetricsProvider {
		/**
		 * Computes the number of grid columns for the layout
		 */
		int getNumberOfGridColumns();
	}
}
