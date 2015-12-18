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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;

import ch.gianulli.flashcards.R;
import ch.gianulli.trelloapi.TrelloList;

public class ListListAdapter extends RecyclerView.Adapter<ListViewHolder> {

	private ArrayList<TrelloList> mLists;

	private ListViewHolder.OnListSelectedListener mListener;

	private int mExpandedCardPosition = -1;

	private ListViewHolder mExpandedCard = null;

	/**
	 * Creates an empty adapter
	 */
	public ListListAdapter(ListViewHolder.OnListSelectedListener listener) {
		this(null, listener);
	}

	/**
	 * Creates an adapter and adds the given lists
	 *
	 * @param lists
	 */
	public ListListAdapter(Collection<TrelloList> lists, ListViewHolder.OnListSelectedListener
			listener) {
		if (lists != null) {
			mLists = new ArrayList<>(lists);
		} else {
			mLists = new ArrayList<>();
		}
		mListener = listener;
	}

	@Override
	public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View v = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
		return new ListViewHolder(v, mLists, mListener, this);
	}

	@Override
	public int getItemViewType(int position) {
		return R.layout.list_item;
	}

	@Override
	public void onBindViewHolder(ListViewHolder holder, int position) {
		holder.bindList(mLists.get(position), mLists);

		if (position == mExpandedCardPosition) {
			holder.expandCard(false);
			mExpandedCard = holder;
		}
	}

	@Override
	public int getItemCount() {
		return mLists.size();
	}

	public boolean isEmpty() {
		return getItemCount() == 0;
	}

	public void setLists(Collection<TrelloList> lists) {
		mLists.clear();
		if (lists != null) {
			mLists.addAll(lists);
		}

		if (mExpandedCard != null) {
			mExpandedCard.collapseCard(true);
		}
		mExpandedCard = null;
		mExpandedCardPosition = -1;

		notifyDataSetChanged();
	}

	/**
	 * Only gets called from {@link ListViewHolder} when a card is expanded by the user.
	 * @param expandedCard
	 */
	public void setExpandedCard(ListViewHolder expandedCard) {
		if (expandedCard != null && mExpandedCard != null) { // collapse other open card
			mExpandedCard.collapseCard(true);
		}
		mExpandedCard = expandedCard;
		if (expandedCard != null) {
			mExpandedCardPosition = expandedCard.getAdapterPosition();
		} else {
			mExpandedCardPosition = -1;
		}
	}

	public void addList(TrelloList list) {
		mLists.add(list);

		if (mExpandedCard != null) {
			mExpandedCard.collapseCard(true);
		}
		mExpandedCard = null;
		mExpandedCardPosition = -1;

		notifyItemInserted(mLists.size() - 1);
	}

}
