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

package ch.gianulli.flashcards;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import ch.gianulli.flashcards.lists.ListListAdapter;
import ch.gianulli.flashcards.lists.ListViewHolder;
import ch.gianulli.flashcards.ui.MarginItemDecoration;
import ch.gianulli.trelloapi.Board;
import ch.gianulli.trelloapi.TrelloAPI;
import ch.gianulli.trelloapi.TrelloList;
import ch.gianulli.trelloapi.TrelloNotAccessibleException;
import ch.gianulli.trelloapi.TrelloNotAuthorizedException;


public class ListsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
		ListViewHolder.OnListSelectedListener, MarginItemDecoration.GridMetricsProvider {

	private static final String KEY_ACTIVE_PAGE = "active_page";

	private static final String KEY_BOARD = "board";

	private TrelloAPI mTrelloApi;

	private ListsFragmentListener mListener;

	private ListListAdapter mAdapter;

	private GridLayoutManager mLayoutManager;

	private RecyclerView mRecyclerView;

	private SwipeRefreshLayout mRefreshLayout;

	private View mProgressIndicator;

	private View mNoListsIndicator;

	private View mTrelloNotAccessibleIndicator;

	private AsyncTask mListLoader = null;

	private Page mActivePage;

	private Board mBoard;

	public ListsFragment() {
	}

	public static ListsFragment newInstance(Board board) {
		ListsFragment result = new ListsFragment();
		Bundle args = new Bundle();
		args.putParcelable(KEY_BOARD, board);
		result.setArguments(args);
		return result;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTrelloApi = new TrelloAPI(getActivity());

		mAdapter = new ListListAdapter(this);

		mBoard = getArguments().getParcelable(KEY_BOARD);

		setRetainInstance(true);
		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof ListsFragmentListener)) {
			throw new IllegalArgumentException("Activity has to implement BoardsFragmentListener");
		} else {
			mListener = (ListsFragmentListener) activity;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_lists, container, false);

		mProgressIndicator = v.findViewById(R.id.progress_indicator);

		mNoListsIndicator = v.findViewById(R.id.no_lists);

		mTrelloNotAccessibleIndicator = v.findViewById(R.id.trello_not_accessible);

		mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.refresh_layout);
		mRefreshLayout.setColorSchemeResources(R.color.color1, R.color.color2, R.color.color3);
		mRefreshLayout.setOnRefreshListener(this);

		mRecyclerView = (RecyclerView) v.findViewById(R.id.recycler_view);
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());
		mLayoutManager = new GridLayoutManager(getActivity(), getNumberOfGridColumns());
		mRecyclerView.addItemDecoration(new MarginItemDecoration(getActivity(), 16, this));
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setAdapter(mAdapter);


		mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
			}


			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				mRefreshLayout.setEnabled(mAdapter.isEmpty() || mLayoutManager
						.findFirstCompletelyVisibleItemPosition() == 0);
			}
		});

		// Restore state
		if (savedInstanceState != null) {
			setActivePage(Page.values()[savedInstanceState.getInt(KEY_ACTIVE_PAGE)]);
		}

		// Set title
		getActivity().setTitle(mBoard.getName());

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		onRefresh();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_lists, menu);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_ACTIVE_PAGE, mActivePage.ordinal());
	}

	@Override
	public void onPause() {
		if (mListLoader != null) {
			mListLoader.cancel(true);
			mListLoader = null;
		}

		// This hack solves a nasty bug that happens when switching fragments during refresh
		if (mRefreshLayout != null) {
			mRefreshLayout.setRefreshing(false);
			mRefreshLayout.destroyDrawingCache();
			mRefreshLayout.clearAnimation();
		}

		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
			case R.id.action_refresh:
				onRefresh();
				break;
			case R.id.action_settings:
				Intent intent = new Intent(getActivity(), SettingsActivity.class);
				startActivity(intent);
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRefresh() {
		if (mListLoader != null) {
			mListLoader.cancel(true);
			mListLoader = null;
		}

		mListLoader = new AsyncTask<Void, Void, ArrayList<TrelloList>>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				if (mAdapter.isEmpty()) {
					setActivePage(Page.PROGRESS);
					mRefreshLayout.setRefreshing(false);
				} else {
					setActivePage(Page.LIST_LIST);
					mRefreshLayout.setRefreshing(true);
				}
			}

			@Override
			protected ArrayList<TrelloList> doInBackground(Void... params) {
				try {
					return mBoard.getAllLists(mTrelloApi);
				} catch (TrelloNotAuthorizedException e) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mTrelloApi.requestAuthorization(getActivity()
									.getSupportFragmentManager());
						}
					});
				} catch (TrelloNotAccessibleException e) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setActivePage(Page.NO_CONNECTION);
						}
					});
				}

				return null;
			}

			@Override
			protected void onPostExecute(ArrayList<TrelloList> lists) {
				mRefreshLayout.setRefreshing(false);

				mAdapter.setLists(lists);
				if (lists != null) {
					if (mAdapter.isEmpty()) {
						setActivePage(Page.NO_BOARDS);
					} else {
						setActivePage(Page.LIST_LIST);
					}
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onListSelected(TrelloList list) {
		mListener.onListSelected(list);
	}

	private void setActivePage(Page page) {
		mActivePage = page;
		mProgressIndicator.setVisibility((page == Page.PROGRESS) ? View.VISIBLE : View.GONE);
		mNoListsIndicator.setVisibility((page == Page.NO_BOARDS) ? View.VISIBLE : View.GONE);
		mTrelloNotAccessibleIndicator.setVisibility((page == Page.NO_CONNECTION) ? View.VISIBLE :
				View.GONE);
		mRecyclerView.setVisibility((page == Page.LIST_LIST) ? View.VISIBLE : View.GONE);
	}

	@Override
	public int getNumberOfGridColumns() {
		return 1;
	}

	public void setListener(ListsFragmentListener listener) {
		mListener = listener;
	}

	private enum Page {LIST_LIST, NO_CONNECTION, NO_BOARDS, PROGRESS}

	public interface ListsFragmentListener {
		void onListSelected(TrelloList list);
	}
}
