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

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import ch.gianulli.flashcards.data.PreferenceModel;
import ch.gianulli.flashcards.ui.Flashcard;
import ch.gianulli.trelloapi.Card;

public class CardsAdapter extends PagerAdapter implements Flashcard.OnCardAnsweredListener {


	private LayoutInflater mInflater;

	private ArrayList<Card> mCards;

	private SparseArray<Boolean> mAnsweredCards;

	private OnCardAnsweredListener mListener;

	private PreferenceModel mPreferenceModel;

	private int mFontSize;

	private boolean mMathMLEnabled;

	public CardsAdapter(Context context, OnCardAnsweredListener listener) {
		this(context, null, listener);
	}


	public CardsAdapter(Context context, ArrayList<Card> cards, OnCardAnsweredListener listener) {
		mInflater = LayoutInflater.from(context);
		if (cards != null) {
			mCards = new ArrayList<>(cards);
		} else {
			mCards = new ArrayList<>();
		}
		mListener = listener;
		mAnsweredCards = new SparseArray<>();

		// Get font size from preferences
		mPreferenceModel = new PreferenceModel(context);
		mFontSize = mPreferenceModel.getFontSize();
		mMathMLEnabled = mPreferenceModel.isMathMLEnabled();
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		Flashcard result = Flashcard.inflateView(mInflater, container, this);
		result.setFontSize(mFontSize);
		result.setMathMLEnabled(mMathMLEnabled);
		result.setCard(mCards.get(position));

		if (mAnsweredCards.indexOfKey(position) >= 0) {
			result.deactivateCard(mAnsweredCards.get(position));
		}

		container.addView(result.getView());
		return result;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		container.removeView(((Flashcard) object).getView());
	}

	@Override
	public int getCount() {
		return mCards.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((Flashcard) object).getView();
	}

	@Override
	public void onCardAnswered(Card card, boolean correctAnswer) {
		int position = mCards.indexOf(card);
		mAnsweredCards.append(position, correctAnswer);
		mListener.onCardAnswered(position, card, correctAnswer);
	}

	/**
	 * @return index of first unanswered card or {@code getCount()} if all cards have been answered
	 */
	public int getFirstUnansweredPosition() {
		for (int i = 0; i < mCards.size(); ++i) {
			if (mAnsweredCards.indexOfKey(i) < 0) {
				return i;
			}
		}
		return mCards.size();
	}

	/**
	 * @return index of next unanswered card after {@code position} or {@code getCount()} if all
	 * following cards have been answered
	 */
	public int getNextUnansweredPosition(int position) {
		for (int i = position + 1; i < mCards.size(); ++i) {
			if (mAnsweredCards.indexOfKey(i) < 0) {
				return i;
			}
		}
		return mCards.size();
	}


	public int getCorrectCardsCount() {
		int result = 0;
		for (int i = 0; i < mAnsweredCards.size(); ++i) {
			if (mAnsweredCards.valueAt(i)) {
				result++;
			}
		}
		return result;
	}

	public void setCards(ArrayList<Card> cards) {
		mCards.clear();
		if (cards != null) {
			mCards.addAll(cards);
		}
		notifyDataSetChanged();
	}

	public interface OnCardAnsweredListener {
		void onCardAnswered(int position, Card card, boolean correctAnswer);
	}
}
