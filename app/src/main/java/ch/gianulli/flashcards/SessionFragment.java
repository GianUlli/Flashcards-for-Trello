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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

import ch.gianulli.flashcards.lists.CardsAdapter;
import ch.gianulli.trelloapi.Card;
import ch.gianulli.trelloapi.TrelloAPI;
import ch.gianulli.trelloapi.TrelloList;
import ch.gianulli.trelloapi.TrelloNotAccessibleException;
import ch.gianulli.trelloapi.TrelloNotAuthorizedException;

public class SessionFragment extends Fragment implements CardsAdapter.OnCardAnsweredListener {

	private static final String KEY_LIST_ID = "list_id";

	private static final String KEY_TITLE = "title";

	private static final String KEY_MODE = "mode";

	private static final String KEY_NBR_OF_CARDS = "cards";

	private static final String KEY_CORRECT_CARDS_LIST_ID = "correct_cards_list_id";

	private static final String KEY_WRONG_CARDS_LIST_ID = "wrong_cards_list_id";

	private ViewPager mViewPager;

	private View mContainer;

	private TextView mPageIndicator;

	private View mProgressIndicator;

	private View mTrelloNotAccessibleIndicator;

	private TrelloAPI mTrelloApi;

	private String mListId;

	/**
	 * Id of list to which correct cards get moved
	 */
	private String mCorrectCardsListId;

	/**
	 * Id of list to which wrong cards get moved
	 */
	private String mWrongCardsListId;

	private TrelloList mList;

	private ArrayList<Card> mCards = null;

	private AsyncTask mCardsLoader = null;

	private CardsAdapter mAdapter;

	public SessionFragment() {
	}

	/**
	 * @param listId
	 * @param mode               one of {@link SessionActivity#MODE_AT_RANDOM}, {@link
	 *                           SessionActivity#MODE_FROM_TOP}, or {@link
	 *                           SessionActivity#MODE_FROM_BOTTOM}
	 * @param nbrOfCards         the number of cards that should be used in the session
	 * @param correctCardsListId Id of list to which correct cards get moved
	 * @param wrongCardsListId   Id of list to which wrong cards get moved
	 * @return
	 */
	public static SessionFragment newInstance(String listId, String title, int mode,
	                                          int nbrOfCards, String correctCardsListId,
	                                          String wrongCardsListId) {
		SessionFragment result = new SessionFragment();
		Bundle args = new Bundle();
		args.putString(KEY_LIST_ID, listId);
		args.putString(KEY_TITLE, title);
		args.putInt(KEY_MODE, mode);
		args.putInt(KEY_NBR_OF_CARDS, nbrOfCards);
		args.putString(KEY_CORRECT_CARDS_LIST_ID, correctCardsListId);
		args.putString(KEY_WRONG_CARDS_LIST_ID, wrongCardsListId);
		result.setArguments(args);
		return result;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTrelloApi = new TrelloAPI(getActivity());

		mListId = getArguments().getString(KEY_LIST_ID);

		mCorrectCardsListId = getArguments().getString(KEY_CORRECT_CARDS_LIST_ID);
		mWrongCardsListId = getArguments().getString(KEY_WRONG_CARDS_LIST_ID);

		mAdapter = new CardsAdapter(getActivity(), this);

		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_session, container, false);

		mProgressIndicator = v.findViewById(R.id.progress_indicator);

		mTrelloNotAccessibleIndicator = v.findViewById(R.id.trello_not_accessible);

		mContainer = v.findViewById(R.id.container);

		mPageIndicator = (TextView) v.findViewById(R.id.page_indicator);

		mViewPager = (ViewPager) v.findViewById(R.id.pager);
		mViewPager.setAdapter(mAdapter);
		mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset,
			                           int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				mPageIndicator.setText(getResources().getString(R.string.page_indicator_text,
						position + 1, mAdapter.getCount()));
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

		if (mCards == null) { // first start
			fetchCards();
		} else {
			setActivePage(Page.SESSION_VIEW);

			// Update page indicator
			mPageIndicator.setText(getResources().getString(R.string.page_indicator_text,
					mViewPager.getCurrentItem() + 1, mAdapter.getCount()));
		}

		getActivity().setTitle(getArguments().getString(KEY_TITLE));

		return v;
	}

	@Override
	public void onCardAnswered(int position, Card card, boolean correctAnswer) {
		// Move card to new list (in background)
		if (correctAnswer && !mCorrectCardsListId.equals(mListId)) {
			card.moveToListAsync(mTrelloApi, mCorrectCardsListId);
		} else if (!correctAnswer && !mWrongCardsListId.equals(mListId)) {
			card.moveToListAsync(mTrelloApi, mWrongCardsListId);
		}

		// Get next card
		int next = mAdapter.getNextUnansweredPosition(position);
		if (next <= mAdapter.getCount() - 1) {
			// Move to next card
			mViewPager.setCurrentItem(next);
		} else {
			next = mAdapter.getFirstUnansweredPosition();
			if (next == mAdapter.getCount()) {
				onSessionFinished();
			} else {
				mViewPager.setCurrentItem(next);
			}
		}
	}

	private void fetchCards() {
		if (mCardsLoader != null) {
			mCardsLoader.cancel(true);
			mCardsLoader = null;
		}

		mCardsLoader = new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				setActivePage(Page.PROGRESS);
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					mList = TrelloList.getList(mTrelloApi, null, mListId);

					if (mList != null) {

						// Select cards
						mCards = mList.getCards();

						int mode = getArguments().getInt(KEY_MODE);
						int n = getArguments().getInt(KEY_NBR_OF_CARDS);
						if (mode == SessionActivity.MODE_AT_RANDOM) {
							mCards = new ArrayList<>(mCards); // cards in list stay untouched
							Collections.shuffle(mCards);
							mCards = new ArrayList<>(mCards.subList(0, Math.min(n, mCards.size()
							)));
						} else if (mode == SessionActivity.MODE_FROM_TOP) {
							mCards = new ArrayList<>(mCards.subList(0, Math.min(n, mCards.size()
							)));
						} else if (mode == SessionActivity.MODE_FROM_BOTTOM) {
							mCards = new ArrayList<>(mCards.subList(Math.max(0, mCards.size() - n)
									, mCards.size()));
							Collections.reverse(mCards);
						}

						return true;
					} else {
						throw new TrelloNotAccessibleException("Result was null.");
					}
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

				return false;
			}

			@Override
			protected void onPostExecute(Boolean success) {
				if (success) {
					mAdapter.setCards(mCards);
					setActivePage(Page.SESSION_VIEW);

					mViewPager.setCurrentItem(0);

					// Update page indicator
					mPageIndicator.setText(getResources().getString(R.string.page_indicator_text,
							mViewPager.getCurrentItem() + 1, mAdapter.getCount()));
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void onSessionFinished() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(getResources().getString(R.string.session_finished, mAdapter
				.getCorrectCardsCount(), mAdapter.getCount()));
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				getActivity().finish();
			}
		});
		builder.show();
	}

	private void setActivePage(Page page) {
		mProgressIndicator.setVisibility((page == Page.PROGRESS) ? View.VISIBLE : View.GONE);
		mTrelloNotAccessibleIndicator.setVisibility((page == Page.NO_CONNECTION) ? View.VISIBLE :
				View.GONE);
		mContainer.setVisibility((page == Page.SESSION_VIEW) ? View.VISIBLE : View.GONE);
	}

	private enum Page {SESSION_VIEW, NO_CONNECTION, PROGRESS}
}
