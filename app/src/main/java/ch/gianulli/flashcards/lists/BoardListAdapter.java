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

import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;

import ch.gianulli.flashcards.R;
import ch.gianulli.trelloapi.Board;

public class BoardListAdapter extends RecyclerView.Adapter<BoardViewHolder> {

	private AppCompatActivity mActivity;

	private ArrayList<Board> mBoards;

	private BoardViewHolder.OnBoardSelectedListener mListener;

	private ActionMode.Callback mActionModeCallback;

	private ActionMode mActionMode = null;

	private boolean mIsSelectable = true;

	private SparseBooleanArray mSelectedPositions = new SparseBooleanArray();

	/**
	 * Creates an empty adapter
	 */
	public BoardListAdapter(AppCompatActivity activity, BoardViewHolder.OnBoardSelectedListener listener, ActionMode.Callback actionModeCallback) {
		this(null, activity, listener, actionModeCallback);
	}

	/**
	 * Creates an adapter and adds the given boards
	 *
	 * @param boards
	 */
	public BoardListAdapter(Collection<Board> boards, AppCompatActivity activity, BoardViewHolder.OnBoardSelectedListener
			listener, ActionMode.Callback actionModeCallback) {
		if (boards != null) {
			mBoards = new ArrayList<>(boards);
		} else {
			mBoards = new ArrayList<>();
		}
		mActivity = activity;
		mListener = listener;
		mActionModeCallback = actionModeCallback;
	}

	@Override
	public BoardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
		return new BoardViewHolder(this, v, mListener);
	}

	@Override
	public int getItemViewType(int position) {
		return R.layout.board_item;
	}

	@Override
	public void onBindViewHolder(BoardViewHolder holder, int position) {
		holder.bindBoard(mBoards.get(position));
	}

	@Override
	public int getItemCount() {
		return mBoards.size();
	}

	public boolean isEmpty() {
		return getItemCount() == 0;
	}

	public void setBoards(Collection<Board> boards) {
		mBoards.clear();
		if (boards != null) {
			mBoards.addAll(boards);
		}
		notifyDataSetChanged();
	}

	public void addBoard(Board board) {
		mBoards.add(board);
		notifyItemInserted(mBoards.size() - 1);
	}

	public void removeBoards(Collection<Board> boards) {
		for (Board board : boards) {
			int index = mBoards.indexOf(board);
			if (index > -1){
				mBoards.remove(index);
				notifyItemRemoved(index);
			}
		}
	}

	public void selectItem(int position) {
		if (areItemsSelectable()) {
			if (!anyItemSelected()) { // first selected item
				mActionMode = mActivity.startSupportActionMode(mActionModeCallback);
				notifyDataSetChanged(); // needed to make images "unclickable"
			}
			if (!isItemSelected(position)) {
				mSelectedPositions.put(position, true);
				notifyItemChanged(position);
				mActionMode.invalidate();
			}
		}
	}

	public void deselectItem(int position) {
		mSelectedPositions.delete(position);
		notifyItemChanged(position);
		mActionMode.invalidate();

		if (!anyItemSelected() && mActionMode != null) {
			mActionMode.finish();
			mActionMode = null;
		}
	}

	public boolean isItemSelected(int position) {
		return mSelectedPositions.get(position);
	}

	public ArrayList<Board> getSelectedItems() {
		ArrayList<Board> result = new ArrayList<>();
		for (int i = 0; i < mBoards.size(); ++i) {
			if (isItemSelected(i)) {
				result.add(mBoards.get(i));
			}
		}
		return result;
	}

	public void setItemsSelectable(boolean selectable) {
		mIsSelectable = selectable;
		if (!selectable) { // delete all selections
			mSelectedPositions.clear();

			if (mActionMode != null) {
				mActionMode.finish();
				mActionMode = null;
			}
		}
		notifyItemRangeChanged(0, mBoards.size()); // all items might have changed
	}

	public boolean areItemsSelectable() {
		return mIsSelectable;
	}

	public boolean anyItemSelected() {
		return mSelectedPositions.size() > 0;
	}

	/** Has to be called when the action mode is finished */
	public void onFinishedActionMode() {
		mActionMode = null;
		mSelectedPositions.clear();
		notifyItemRangeChanged(0, mBoards.size()); // all items might have changed
	}
}
