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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Set;

import ch.gianulli.flashcards.data.PreferenceModel;
import ch.gianulli.flashcards.lists.BoardListAdapter;
import ch.gianulli.flashcards.lists.BoardViewHolder;
import ch.gianulli.flashcards.ui.MarginItemDecoration;
import ch.gianulli.trelloapi.Board;
import ch.gianulli.trelloapi.TrelloAPI;
import ch.gianulli.trelloapi.TrelloNotAccessibleException;
import ch.gianulli.trelloapi.TrelloNotAuthorizedException;


public class BoardsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
		BoardViewHolder.OnBoardSelectedListener, MarginItemDecoration.GridMetricsProvider {

	private static final String KEY_ACTIVE_PAGE = "active_page";

	private TrelloAPI mTrelloApi;

	private PreferenceModel mPrefModel;

	private BoardsFragmentListener mListener;

	private BoardListAdapter mAdapter;

	private GridLayoutManager mLayoutManager;

	private RecyclerView mRecyclerView;

	private SwipeRefreshLayout mRefreshLayout;

	private View mProgressIndicator;

	private View mNoBoardsIndicator;

	private View mTrelloNotAccessibleIndicator;

	private AsyncTask mBoardLoader = null;

	private Page mActivePage;

	/**
	 * Enables item selection
	 */
	private ActionMode.Callback mSelectItemsCallback = new ActionMode.Callback() {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			if (mAdapter.areItemsSelectable()) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.context_boards, menu);
				return true;
			}
			return false;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return true; // Return false if nothing is done
		}

		@Override
		public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
			final ArrayList<Board> selectedBoards = mAdapter.getSelectedItems();
			switch (item.getItemId()) {
				case R.id.action_hide:
					// Hide selected boards
					if (selectedBoards != null && selectedBoards.size() > 0) {
						String[] ids = new String[selectedBoards.size()];
						for (int i = 0; i < selectedBoards.size(); ++i) {
							ids[i] = selectedBoards.get(i).getId();
						}
						mPrefModel.addHiddenBoards(ids);
						mAdapter.removeBoards(selectedBoards);
						mode.finish(); // Action picked, so close the CAB
						return true;
					}
					break;
			}
			return false;
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mAdapter.onFinishedActionMode();
		}
	};

	public BoardsFragment() {
	}

	public static BoardsFragment newInstance() {
		return new BoardsFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTrelloApi = new TrelloAPI(getActivity());

		mPrefModel = new PreferenceModel(getActivity());

		mAdapter = new BoardListAdapter((AppCompatActivity) getActivity(), this,
				mSelectItemsCallback);

		setRetainInstance(true);
		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof BoardsFragmentListener)) {
			throw new IllegalArgumentException("Activity has to implement BoardsFragmentListener");
		} else {
			mListener = (BoardsFragmentListener) activity;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_boards, container, false);

		mProgressIndicator = v.findViewById(R.id.progress_indicator);

		mNoBoardsIndicator = v.findViewById(R.id.no_boards);

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
		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_ACTIVE_PAGE)) {
			setActivePage(Page.values()[savedInstanceState.getInt(KEY_ACTIVE_PAGE)]);
		}

		// Set title
		getActivity().setTitle(R.string.title_activity_main);

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
		inflater.inflate(R.menu.menu_boards, menu);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivePage != null) {
			outState.putInt(KEY_ACTIVE_PAGE, mActivePage.ordinal());
		}
	}

	@Override
	public void onPause() {
		if (mBoardLoader != null) {
			mBoardLoader.cancel(true);
			mBoardLoader = null;
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
		if (mBoardLoader != null) {
			mBoardLoader.cancel(true);
			mBoardLoader = null;
		}

		mBoardLoader = new AsyncTask<Void, Void, ArrayList<Board>>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				if (mAdapter.isEmpty()) {
					setActivePage(Page.PROGRESS);
					mRefreshLayout.setRefreshing(false);
				} else {
					setActivePage(Page.BOARD_LIST);
					mRefreshLayout.setRefreshing(true);
				}
			}

			@Override
			protected ArrayList<Board> doInBackground(Void... params) {
				try {
					// Get boards from Trello
					ArrayList<Board> result = Board.listAllBoards(mTrelloApi);

					// Remove hidden boards
					Set<String> hiddenBoards = mPrefModel.getHiddenBoards();
					for (int i = result.size() - 1; i >= 0; --i) {
						String id = result.get(i).getId();
						if (hiddenBoards.contains(id)) {
							result.remove(i);
						}
					}

					return result;
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
					Log.d("test", "Trello not accessible: " + e.getMessage());
				}

				return null;
			}

			@Override
			protected void onPostExecute(ArrayList<Board> boards) {
				mRefreshLayout.setRefreshing(false);

				mAdapter.setBoards(boards);
				if (boards != null) {
					if (mAdapter.isEmpty()) {
						setActivePage(Page.NO_BOARDS);
					} else {
						setActivePage(Page.BOARD_LIST);
					}
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void onBoardSelected(Board board) {
		mListener.onBoardSelected(board);
	}

	private void setActivePage(Page page) {
		mActivePage = page;
		mProgressIndicator.setVisibility((page == Page.PROGRESS) ? View.VISIBLE : View.GONE);
		mNoBoardsIndicator.setVisibility((page == Page.NO_BOARDS) ? View.VISIBLE : View.GONE);
		mTrelloNotAccessibleIndicator.setVisibility((page == Page.NO_CONNECTION) ? View.VISIBLE :
				View.GONE);
		mRecyclerView.setVisibility((page == Page.BOARD_LIST) ? View.VISIBLE : View.GONE);
	}

	@Override
	public int getNumberOfGridColumns() {
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int widthDp = (int) Math.ceil(metrics.widthPixels / metrics.density);
		return widthDp / 280;
	}

	public void setListener(BoardsFragmentListener listener) {
		mListener = listener;
	}

	private enum Page {BOARD_LIST, NO_CONNECTION, NO_BOARDS, PROGRESS}

	public interface BoardsFragmentListener {
		void onBoardSelected(Board board);
	}
}
