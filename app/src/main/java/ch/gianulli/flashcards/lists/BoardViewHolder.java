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

package ch.gianulli.flashcards.lists;

import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.balysv.materialripple.MaterialRippleLayout;

import ch.gianulli.flashcards.R;
import ch.gianulli.trelloapi.Board;

public class BoardViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
		View.OnLongClickListener {

	private static final int DARK_TEXT_COLOR = 0xDE000000; // 87% black

	private static final int BRIGHT_TEXT_COLOR = 0xFFFFFFFF; // white

	private TextView mTextView;

	private CardView mCard;

	private MaterialRippleLayout mRipple;

	private Board mBoard;

	private BoardListAdapter mAdapter;

	private OnBoardSelectedListener mListener;

	public BoardViewHolder(BoardListAdapter adapter, View itemView,
	                       OnBoardSelectedListener listener) {
		super(itemView);

		mAdapter = adapter;

		mCard = (CardView) itemView;
		mRipple = (MaterialRippleLayout) itemView.findViewById(R.id.ripple);
		mTextView = (TextView) itemView.findViewById(android.R.id.text1);

		mCard.setPreventCornerOverlap(false);

		mRipple.setOnClickListener(this);
		mRipple.setOnLongClickListener(this);

		mListener = listener;
	}

	public void bindBoard(Board board) {
		mBoard = board;

		mCard.setActivated(mAdapter.isItemSelected(getAdapterPosition()));

		mCard.setCardBackgroundColor(board.getColor());

		if (((Color.red(board.getColor()) * 299) + (Color.green(board.getColor()) * 587) + (Color
				.blue(board.getColor()) * 114)) / 1000 >= 125) { // bright background --> dark text
			mTextView.setTextColor(DARK_TEXT_COLOR);
		} else {
			mTextView.setTextColor(BRIGHT_TEXT_COLOR);
		}

		mTextView.setText(board.getName());
	}

	@Override
	public void onClick(View v) {
		if (!mAdapter.anyItemSelected()) {
			mListener.onBoardSelected(mBoard);
		} else {
			int position = getAdapterPosition();
			if (mAdapter.isItemSelected(position)) {
				mAdapter.deselectItem(position);
			} else {
				mAdapter.selectItem(position);
			}
		}
	}

	@Override
	public boolean onLongClick(View v) {
		int position = getAdapterPosition();
		if (mAdapter.isItemSelected(position)) {
			mAdapter.deselectItem(position);
		} else {
			mAdapter.selectItem(position);
		}
		return true;
	}

	public interface OnBoardSelectedListener {
		void onBoardSelected(Board board);
	}

}
