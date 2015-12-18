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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import ch.gianulli.flashcards.R;
import ch.gianulli.flashcards.SessionActivity;
import ch.gianulli.flashcards.data.PreferenceModel;
import ch.gianulli.flashcards.ui.TouchDelegateComposite;
import ch.gianulli.trelloapi.TrelloList;

public class ListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

	private ListListAdapter mAdapter;

	private Context mContext;

	private TextView mTextTitle;

	private TextView mTextDescription;

	private Spinner mCardSelectionModeSpinner;

	private Spinner mCardAmountSpinner;

	private Spinner mCorrectCardsListSpinner;

	private Spinner mWrongCardsListSpinner;

	private Button mStartSessionButton;

	private CardView mCard;

	private LinearLayout mSessionLayout;

	private FrameLayout mContainer;

	private TrelloList mList;

	private ArrayList<String> mAllListNames = new ArrayList<>();

	private ArrayList<String> mAllListIds = new ArrayList<>();

	private int mThisListPosition = 0;

	private ArrayAdapter<String> mCardAmountSpinnerAdapter;

	private ArrayAdapter<String> mCorrectCardsListSpinnerAdapter;

	private ArrayAdapter<String> mWrongCardsListSpinnerAdapter;

	private String[] mCardAmounts;

	private OnListSelectedListener mListener;

	private Animator mPreviousAnimation = null;

	private boolean mExpanded = false;

	private PreferenceModel mPrefModel;

	public ListViewHolder(View itemView, ArrayList<TrelloList> allLists,
	                      OnListSelectedListener listener, ListListAdapter adapter) {
		super(itemView);

		mAdapter = adapter;
		mContext = itemView.getContext();
		mCardAmounts = mContext.getResources().getStringArray(R.array.card_amount);
		mPrefModel = new PreferenceModel(mContext);

		mCard = (CardView) itemView;
		mContainer = (FrameLayout) itemView.findViewById(R.id.container);
		mTextTitle = (TextView) itemView.findViewById(android.R.id.text1);
		mTextDescription = (TextView) itemView.findViewById(android.R.id.text2);
		mCardSelectionModeSpinner = (Spinner) itemView.findViewById(R.id
				.card_selection_mode_spinner);
		mCardAmountSpinner = (Spinner) itemView.findViewById(R.id.card_amount_spinner);
		mCorrectCardsListSpinner = (Spinner) itemView.findViewById(R.id.correct_card_list_spinner);
		mWrongCardsListSpinner = (Spinner) itemView.findViewById(R.id.wrong_card_list_spinner);
		mStartSessionButton = (Button) itemView.findViewById(R.id.start_session_button);

		mCard.setPreventCornerOverlap(false);

		mContainer.setOnClickListener(this);

		ArrayAdapter<CharSequence> cardSelectionModeSpinnerAdapter = ArrayAdapter
				.createFromResource(mContext, R.array.card_selection_modes, android.R.layout
						.simple_spinner_item);
		cardSelectionModeSpinnerAdapter.setDropDownViewResource(android.R.layout
				.simple_spinner_dropdown_item);
		mCardSelectionModeSpinner.setAdapter(cardSelectionModeSpinnerAdapter);

		mCardAmountSpinnerAdapter = new ArrayAdapter<>(mContext, android.R.layout
				.simple_spinner_dropdown_item, new ArrayList<>(Arrays.asList(mCardAmounts)));
		mCardAmountSpinnerAdapter.setDropDownViewResource(android.R.layout
				.simple_spinner_dropdown_item);
		mCardAmountSpinner.setAdapter(mCardAmountSpinnerAdapter);

		mCorrectCardsListSpinnerAdapter = new ArrayAdapter<>(mContext, android.R.layout
				.simple_spinner_dropdown_item, mAllListNames);
		mCorrectCardsListSpinnerAdapter.setDropDownViewResource(android.R.layout
				.simple_spinner_dropdown_item);
		mCorrectCardsListSpinner.setAdapter(mCorrectCardsListSpinnerAdapter);

		mWrongCardsListSpinnerAdapter = new ArrayAdapter<>(mContext, android.R.layout
				.simple_spinner_dropdown_item, mAllListNames);
		mWrongCardsListSpinnerAdapter.setDropDownViewResource(android.R.layout
				.simple_spinner_dropdown_item);
		mWrongCardsListSpinner.setAdapter(mWrongCardsListSpinnerAdapter);

		mSessionLayout = (LinearLayout) itemView.findViewById(R.id.session_layout);

		// Extend touchable area of spinners
		final LinearLayout settingsLayout = (LinearLayout) itemView.findViewById(R.id
				.amount_settings_layout);
		settingsLayout.post(new Runnable() {

			@Override
			public void run() {
				TouchDelegateComposite delegates = new TouchDelegateComposite(settingsLayout);
				final int padding = (int) Math.ceil(TypedValue.applyDimension(TypedValue
						.COMPLEX_UNIT_DIP, 6, mContext.getResources().getDisplayMetrics())); // 6dp

				Rect delegateArea1 = new Rect();
				mCardSelectionModeSpinner.getHitRect(delegateArea1);
				// 36dp --> 48dp
				delegateArea1.top -= padding;
				delegateArea1.bottom += padding;
				TouchDelegate touchDelegate1 = new TouchDelegate(delegateArea1,
						mCardSelectionModeSpinner);

				Rect delegateArea2 = new Rect();
				mCardAmountSpinner.getHitRect(delegateArea2);
				// 36dp --> 48dp
				delegateArea2.top -= padding;
				delegateArea2.bottom += padding;
				TouchDelegate touchDelegate2 = new TouchDelegate(delegateArea2,
						mCardAmountSpinner);

				delegates.addDelegate(touchDelegate1);
				delegates.addDelegate(touchDelegate2);

				settingsLayout.setTouchDelegate(delegates);
			}
		});
		final LinearLayout correctCardsListLayout = (LinearLayout) itemView.findViewById(R.id
				.correct_card_settings_layout);
		correctCardsListLayout.post(new Runnable() {

			@Override
			public void run() {
				final int padding = (int) Math.ceil(TypedValue.applyDimension(TypedValue
						.COMPLEX_UNIT_DIP, 6, mContext.getResources().getDisplayMetrics())); // 6dp

				Rect delegateArea1 = new Rect();
				mCorrectCardsListSpinner.getHitRect(delegateArea1);
				// 36dp --> 48dp
				delegateArea1.top -= padding;
				delegateArea1.bottom += padding;
				TouchDelegate touchDelegate = new TouchDelegate(delegateArea1,
						mCorrectCardsListSpinner);

				correctCardsListLayout.setTouchDelegate(touchDelegate);
			}
		});final LinearLayout wrongCardsListLayout = (LinearLayout) itemView.findViewById(R.id
				.wrong_card_settings_layout);
		wrongCardsListLayout.post(new Runnable() {

			@Override
			public void run() {
				final int padding = (int) Math.ceil(TypedValue.applyDimension(TypedValue
						.COMPLEX_UNIT_DIP, 6, mContext.getResources().getDisplayMetrics())); // 6dp

				Rect delegateArea1 = new Rect();
				mWrongCardsListSpinner.getHitRect(delegateArea1);
				// 36dp --> 48dp
				delegateArea1.top -= padding;
				delegateArea1.bottom += padding;
				TouchDelegate touchDelegate = new TouchDelegate(delegateArea1,
						mWrongCardsListSpinner);

				wrongCardsListLayout.setTouchDelegate(touchDelegate);
			}
		});

		mStartSessionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Save selected positions
				mPrefModel.setCardAmountPosition(mCardAmountSpinner.getSelectedItemPosition());
				mPrefModel.setCardSelectionModePosition(mCardSelectionModeSpinner
						.getSelectedItemPosition());

				// Open new activity
				int mode = mCardSelectionModeSpinner.getSelectedItemPosition();
				int n = Integer.parseInt(mCardAmountSpinnerAdapter.getItem(mCardAmountSpinner
						.getSelectedItemPosition()));
				String correctCardsListId = mAllListIds.get(mCorrectCardsListSpinner
						.getSelectedItemPosition());
				String wrongCardsListId = mAllListIds.get(mWrongCardsListSpinner
						.getSelectedItemPosition());

				Intent intent = new Intent(mContext, SessionActivity.class);
				intent.putExtra(SessionActivity.KEY_LIST_ID, mList.getId());
				intent.putExtra(SessionActivity.KEY_TITLE, mList.getName());
				intent.putExtra(SessionActivity.KEY_MODE, mode);
				intent.putExtra(SessionActivity.KEY_NBR_OF_CARDS, n);
				intent.putExtra(SessionActivity.KEY_CORRECT_CARDS_LIST_ID, correctCardsListId);
				intent.putExtra(SessionActivity.KEY_WRONG_CARDS_LIST_ID, wrongCardsListId);

				mContext.startActivity(intent);
			}
		});

		mListener = listener;
	}

	public void bindList(TrelloList list, ArrayList<TrelloList> allLists) {
		mList = list;

		mTextTitle.setText(list.getName());

		int cards = list.getCards().size();
		if (cards == 0) {
			mTextDescription.setText(mContext.getString(R.string.no_cards));
		}
		if (cards == 1) {
			mTextDescription.setText(mContext.getString(R.string.one_card));
		} else {
			mTextDescription.setText(String.format(mContext.getString(R.string.some_cards),
					cards));
		}

		// Update card amount
		int i = 0;
		for (; i < mCardAmounts.length; ++i) {
			if (Integer.parseInt(mCardAmounts[i]) >= list.getCards().size()) {
				break;
			}
		}
		String[] amounts = Arrays.copyOf(mCardAmounts, i + 1);
		amounts[i] = String.valueOf(list.getCards().size());
		mCardAmountSpinnerAdapter.clear();
		mCardAmountSpinnerAdapter.addAll(amounts);

		// Get all list names and ids
		mAllListIds.clear();
		mAllListNames.clear();
		for (TrelloList l : allLists) {
			mAllListIds.add(l.getId());
			mAllListNames.add(l.getName());
		}
		mThisListPosition = mAllListIds.indexOf(mList.getId());

		Log.d("Test", "ids: " + mAllListIds.size() + ", names: " + mAllListNames + ", position: " + mThisListPosition);
		Log.d("Test", mAllListNames.toString());

		// Update list spinners
		mCorrectCardsListSpinnerAdapter.notifyDataSetChanged();
		mWrongCardsListSpinnerAdapter.notifyDataSetChanged();

		// Select correct items
		mCardSelectionModeSpinner.setSelection(mPrefModel.getCardSelectionModePosition());
		mCardAmountSpinner.setSelection(Math.min(mCardAmountSpinnerAdapter.getCount() - 1,
				mPrefModel.getCardAmountPosition()));
		mCorrectCardsListSpinner.setSelection(mThisListPosition);
		mWrongCardsListSpinner.setSelection(mThisListPosition);

		mExpanded = false;
		collapseCard(false);
	}

	@Override
	public void onClick(View v) {
		if (!mExpanded) {
			expandCard(true);
			mAdapter.setExpandedCard(this);
		} else {
			collapseCard(true);
			mAdapter.setExpandedCard(null);
		}
	}

	public void expandCard(boolean animate) {
		mExpanded = true;

		if (mPreviousAnimation != null) {
			mPreviousAnimation.cancel();
			mPreviousAnimation = null;
		}

		if (mSessionLayout.getVisibility() == View.GONE) {
			mSessionLayout.setVisibility(View.VISIBLE);
			mSessionLayout.setAlpha(1.0f);

			if (animate) {
				mSessionLayout.setAlpha(0.0f);

				final int startingHeight = mContainer.getHeight();

				final ViewTreeObserver observer = mContainer.getViewTreeObserver();
				observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw() {
						// We don't want to continue getting called for every listview drawing.
						if (observer.isAlive()) {
							observer.removeOnPreDrawListener(this);
						}

						final int endingHeight = mContainer.getHeight();
						final int distance = endingHeight - startingHeight;

						mContainer.getLayoutParams().height = startingHeight;

						mContainer.requestLayout();

						ValueAnimator heightAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration
								(300);

						heightAnimator.setInterpolator(new DecelerateInterpolator());
						heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener
								() {
							@Override
							public void onAnimationUpdate(ValueAnimator animator) {
								Float value = (Float) animator.getAnimatedValue();
								mContainer.getLayoutParams().height = (int) (value * distance +
										startingHeight);
								mContainer.requestLayout();
							}
						});
						heightAnimator.addListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								mContainer.getLayoutParams().height = ViewGroup.LayoutParams
										.WRAP_CONTENT;
							}
						});

						mSessionLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
						ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mSessionLayout,
								"alpha", 0.0f, 1.0f);
						alphaAnimator.setInterpolator(new DecelerateInterpolator());
						alphaAnimator.setDuration(300);
						alphaAnimator.setStartDelay(100);

						AnimatorSet set = new AnimatorSet();
						set.playTogether(heightAnimator, alphaAnimator);
						set.addListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								mPreviousAnimation = null;
								mSessionLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
							}
						});

						mPreviousAnimation = set;

						set.start();

						return false;
					}
				});
			}
		}
	}

	public void collapseCard(boolean animate) {
		mExpanded = false;

		if (mPreviousAnimation != null) {
			mPreviousAnimation.cancel();
			mPreviousAnimation = null;
		}

		if (mSessionLayout.getVisibility() == View.VISIBLE) {
			mSessionLayout.setVisibility(View.GONE);
			if (animate) {
				final int startingHeight = mContainer.getHeight();

				final ViewTreeObserver observer = mContainer.getViewTreeObserver();
				observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw() {
						// We don't want to continue getting called for every listview drawing.
						if (observer.isAlive()) {
							observer.removeOnPreDrawListener(this);
						}

						final int endingHeight = mContainer.getHeight();
						final int distance = endingHeight - startingHeight;

						mSessionLayout.setVisibility(View.VISIBLE);
						mContainer.getLayoutParams().height = startingHeight;

						mContainer.requestLayout();

						ValueAnimator heightAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration
								(300);

						heightAnimator.setInterpolator(new DecelerateInterpolator());
						heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener
								() {
							@Override
							public void onAnimationUpdate(ValueAnimator animator) {
								Float value = (Float) animator.getAnimatedValue();
								mContainer.getLayoutParams().height = (int) (value * distance +
										startingHeight);
								mContainer.requestLayout();
							}
						});

						ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mSessionLayout,
								"alpha", 1.0f, 0.0f);
						alphaAnimator.setDuration(200);
						alphaAnimator.setInterpolator(new DecelerateInterpolator());

						AnimatorSet set = new AnimatorSet();
						set.playTogether(heightAnimator, alphaAnimator);
						set.addListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								mContainer.getLayoutParams().height = ViewGroup.LayoutParams
										.WRAP_CONTENT;
								mSessionLayout.setVisibility(View.GONE);
								mPreviousAnimation = null;
							}
						});

						mPreviousAnimation = set;

						set.start();

						return false;
					}
				});
			}
		}
	}

	public interface OnListSelectedListener {
		void onListSelected(TrelloList list);
	}

}
