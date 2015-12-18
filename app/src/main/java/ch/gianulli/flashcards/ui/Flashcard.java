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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.FrameLayout;

import ch.gianulli.flashcards.R;
import ch.gianulli.trelloapi.Card;

/**
 * Wrapper class for flashcard.xml
 */
public class Flashcard {

	private static String sDefaultTextColor = "#000000";

	private static String sDeactivatedTextColor = "#000000";

	private static String sGreenTextColor = "#000000";

	private Card mCard;

	private View mView;

	private StyledMarkdownView mQuestion;

	private StyledMarkdownView mAnswer;

	private CardView mCardView;

	private View mButtonBar;

	private Button mCorrectButton;

	private Button mWrongButton;

	private Context mContext;

	private String mQuestionText;

	private String mQuestionColor;

	private String mAnswerText;

	private String mAnswerColor;

	private boolean mQuestionShowing = true;

	private boolean mButtonBarShowing = false;

	private Animator mPreviousAnimation = null;

	private OnCardAnsweredListener mListener;

	// Code from http://stackoverflow.com/questions/5116909/how-i-can-get-onclick-event-on-webview
	// -in-android
	private View.OnTouchListener mTurnCardListener = new View.OnTouchListener() {

		public final static int FINGER_RELEASED = 0;

		public final static int FINGER_TOUCHED = 1;

		public final static int FINGER_DRAGGING = 2;

		public final static int FINGER_UNDEFINED = 3;

		private int fingerState = FINGER_RELEASED;


		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {

			switch (motionEvent.getAction()) {

				case MotionEvent.ACTION_DOWN:
					if (fingerState == FINGER_RELEASED) {
						fingerState = FINGER_TOUCHED;
					} else {
						fingerState = FINGER_UNDEFINED;
					}
					break;

				case MotionEvent.ACTION_UP:
					if (fingerState != FINGER_DRAGGING) {
						fingerState = FINGER_RELEASED;

						turnCard();

						return true;

					} else if (fingerState == FINGER_DRAGGING) {
						fingerState = FINGER_RELEASED;
					} else {
						fingerState = FINGER_UNDEFINED;
					}
					break;

				case MotionEvent.ACTION_MOVE:
					if (fingerState == FINGER_TOUCHED || fingerState == FINGER_DRAGGING) {
						fingerState = FINGER_DRAGGING;
					} else {
						fingerState = FINGER_UNDEFINED;
					}
					break;

				default:
					fingerState = FINGER_UNDEFINED;

			}

			return false;
		}
	};

	/**
	 * Font size measured in sp
	 */
	private int mFontSize = 20;

	private boolean mMathMLEnabled = false;

	/**
	 * @param view Instance of flashcard.xml
	 */
	public Flashcard(View view, OnCardAnsweredListener listener) {
		mListener = listener;

		mView = view;
		mQuestion = (StyledMarkdownView) view.findViewById(R.id.question);
		mAnswer = (StyledMarkdownView) view.findViewById(R.id.answer);
		mCardView = (CardView) view.findViewById(R.id.card);
		mButtonBar = view.findViewById(R.id.button_bar);
		mCorrectButton = (Button) view.findViewById(R.id.correct_button);
		mWrongButton = (Button) view.findViewById(R.id.wrong_button);

		mContext = mView.getContext();

		// Load colors
		int[] attrs = {android.R.attr.textColorSecondary, android.R.attr.textColorPrimary};
		TypedArray ta = mView.getContext().obtainStyledAttributes(R.style.AppTheme, attrs);
		sDeactivatedTextColor = colorToCSSString(ta.getColor(0, 0));
		sDefaultTextColor = colorToCSSString(ta.getColor(1, 0));
		ta.recycle();

		sGreenTextColor = colorToCSSString(ContextCompat.getColor(mContext, R.color.green));

		mQuestionColor = sDefaultTextColor;
		mAnswerColor = sGreenTextColor;

		// Make question visible
		mQuestion.setAlpha(1.0f);
		mAnswer.setAlpha(0.0f);

		// Setup WebViews
		WebSettings settings = mQuestion.getSettings();
		settings.setDefaultFontSize(mFontSize);
		settings.setLoadsImagesAutomatically(true);
		settings.setGeolocationEnabled(false);
		settings.setAllowFileAccess(false);
		settings.setDisplayZoomControls(false);
		settings.setNeedInitialFocus(false);
		settings.setSupportZoom(false);
		settings.setSaveFormData(false);
		settings.setJavaScriptEnabled(true);
		mQuestion.setHorizontalScrollBarEnabled(false);
		mQuestion.setVerticalScrollBarEnabled(false);

		settings = mAnswer.getSettings();
		settings.setDefaultFontSize(mFontSize);
		settings.setLoadsImagesAutomatically(true);
		settings.setGeolocationEnabled(false);
		settings.setAllowFileAccess(false);
		settings.setDisplayZoomControls(false);
		settings.setNeedInitialFocus(false);
		settings.setSupportZoom(false);
		settings.setSaveFormData(false);
		settings.setJavaScriptEnabled(true);
		mAnswer.setHorizontalScrollBarEnabled(false);
		mAnswer.setVerticalScrollBarEnabled(false);

		// Hack to disable text selection in WebViews
		mQuestion.setOnLongClickListener(new View.OnLongClickListener() {

			public boolean onLongClick(View v) {
				return true;
			}
		});
		mAnswer.setOnLongClickListener(new View.OnLongClickListener() {

			public boolean onLongClick(View v) {
				return true;
			}
		});

		// Card should "turn" on click
		final FrameLayout questionLayout = (FrameLayout) view.findViewById(R.id.question_layout);
		questionLayout.setClickable(true);
		questionLayout.setOnTouchListener(mTurnCardListener);

		mQuestion.setOnTouchListener(mTurnCardListener);
		mAnswer.setOnTouchListener(mTurnCardListener);

		// Deactivate card when user answers it
		mCorrectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deactivateCard(true);
				mListener.onCardAnswered(mCard, true);
			}
		});
		mWrongButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deactivateCard(false);
				mListener.onCardAnswered(mCard, false);
			}
		});

		// Limit card width to 400dp
		ViewTreeObserver observer = mCardView.getViewTreeObserver();
		final int width480dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400,
				view.getContext().getResources().getDisplayMetrics());
		observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				if (mCardView.getWidth() > width480dp) {
					ViewGroup.LayoutParams layoutParams = mCardView.getLayoutParams();
					layoutParams.width = width480dp;
					mCardView.setLayoutParams(layoutParams);
					mCardView.requestLayout();

					return false;
				}
				return true;
			}
		});
	}

	/**
	 * Inflates a new version of flashcard.xml
	 *
	 * @param inflater
	 * @param root
	 * @return wrapper for inflated view
	 */
	public static Flashcard inflateView(LayoutInflater inflater, ViewGroup root,
	                                    OnCardAnsweredListener listener) {
		return new Flashcard(inflater.inflate(R.layout.flashcard, root, false), listener);
	}

	/**
	 * Converts a color into CSS format
	 *
	 * @param color
	 * @return
	 */
	private static String colorToCSSString(int color) {
		return String.format("rgba(%d,%d,%d,%.2f)", Color.red(color), Color.green(color), Color
				.blue
						(color), Color.alpha(color) / 255.0);
	}

	/**
	 * @param question String containing Markdown text
	 */
	private void setQuestion(String question) {
		if (mMathMLEnabled) {
			mQuestion.loadMarkdownWithMathML(question, mQuestionColor);
		} else {
			mQuestion.loadMarkdownWithColor(question, mQuestionColor);
		}
		mQuestionText = question;
	}

	/**
	 * @param answer String containing Markdown text
	 */
	private void setAnswer(String answer) {
		if (mMathMLEnabled) {
			mAnswer.loadMarkdownWithMathML(answer, mAnswerColor);
		} else {
			mAnswer.loadMarkdownWithColor(answer, mAnswerColor);
		}
		mAnswerText = answer;
	}

	public View getView() {
		return mView;
	}

	public void setCard(Card card) {
		mCard = card;
		setQuestion(card.getName());
		setAnswer(card.getDesc());
	}

	public void turnCard() {
		if (mPreviousAnimation != null) {
			mPreviousAnimation.cancel();
			mPreviousAnimation = null;
		}

		AnimatorSet set = new AnimatorSet();

		mQuestion.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mAnswer.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		if (mQuestionShowing) {
			mQuestionShowing = false;

			mQuestion.setVisibility(View.VISIBLE);
			mQuestion.setAlpha(1.0f);
			mAnswer.setVisibility(View.VISIBLE);
			mAnswer.setAlpha(0.0f);

			ObjectAnimator questionAnim = ObjectAnimator.ofFloat(mQuestion, "alpha", 1.0f, 0.0f);
			questionAnim.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mQuestion.setVisibility(View.GONE);
				}
			});

			ObjectAnimator answerAnim = ObjectAnimator.ofFloat(mAnswer, "alpha", 0.0f, 1.0f);

			set.playTogether(questionAnim, answerAnim);

			// Show button bar if necessary
			if (!mButtonBarShowing) {
				expandButtonBar();
			}
		} else {
			mQuestionShowing = true;

			mQuestion.setVisibility(View.VISIBLE);
			mQuestion.setAlpha(0.0f);
			mAnswer.setVisibility(View.VISIBLE);
			mAnswer.setAlpha(1.0f);

			ObjectAnimator questionAnim = ObjectAnimator.ofFloat(mQuestion, "alpha", 0.0f, 1.0f);

			ObjectAnimator answerAnim = ObjectAnimator.ofFloat(mAnswer, "alpha", 1.0f, 0.0f);
			answerAnim.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mAnswer.setVisibility(View.GONE);
				}
			});

			set.playTogether(questionAnim, answerAnim);
		}

		set.setDuration(400);

		mPreviousAnimation = set;

		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mQuestion.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
				mAnswer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
				mPreviousAnimation = null;
			}
		});

		set.start();
	}

	private void expandButtonBar() {
		mButtonBarShowing = true;

		mButtonBar.setVisibility(View.VISIBLE);
		mButtonBar.setAlpha(0.0f);

		final int startingHeight = mCardView.getHeight();

		final ViewTreeObserver observer = mCardView.getViewTreeObserver();
		observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				// We don't want to continue getting called for every listview drawing.
				if (observer.isAlive()) {
					observer.removeOnPreDrawListener(this);
				}

				final int endingHeight = mCardView.getHeight();
				final int distance = endingHeight - startingHeight;

				mCardView.getLayoutParams().height = startingHeight;

				mCardView.requestLayout();

				ValueAnimator heightAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration
						(300);

				heightAnimator.setInterpolator(new DecelerateInterpolator());
				heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener
						() {
					@Override
					public void onAnimationUpdate(ValueAnimator animator) {
						Float value = (Float) animator.getAnimatedValue();
						mCardView.getLayoutParams().height = (int) (value * distance +
								startingHeight);
						mCardView.requestLayout();
					}
				});
				heightAnimator.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mCardView.getLayoutParams().height = ViewGroup.LayoutParams
								.WRAP_CONTENT;
					}
				});

				mButtonBar.setLayerType(View.LAYER_TYPE_HARDWARE, null);
				ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(mButtonBar, "alpha",
						0.0f, 1.0f);
				alphaAnimator.setInterpolator(new DecelerateInterpolator());
				alphaAnimator.setDuration(300);
				alphaAnimator.setStartDelay(100);

				AnimatorSet set = new AnimatorSet();
				set.playTogether(heightAnimator, alphaAnimator);
				set.addListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mButtonBar.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
					}
				});

				set.start();

				return false;
			}
		});
	}

	public void deactivateCard(boolean correctAnswer) {
		// Show button bar
		mButtonBarShowing = true;
		mButtonBar.setVisibility(View.VISIBLE);
		mButtonBar.setAlpha(1.0f);

		// Set correct text color
		mQuestionColor = sDeactivatedTextColor;
		mAnswerColor = sDeactivatedTextColor;
		setQuestion(mQuestionText);
		setAnswer(mAnswerText);

		// Deactivate buttons
		mCorrectButton.setEnabled(false);
		mCorrectButton.setTypeface((correctAnswer) ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
		mWrongButton.setEnabled(false);
		mWrongButton.setTypeface((correctAnswer) ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);
	}

	public boolean isMathMLEnabled() {
		return mMathMLEnabled;
	}

	public void setMathMLEnabled(boolean mathMLEnabled) {
		mMathMLEnabled = mathMLEnabled;

		// Update views
		setQuestion(mQuestionText);
		setAnswer(mAnswerText);
	}

	/**
	 * @return the font size measured in sp
	 */
	public int getFontSize() {
		return mFontSize;
	}

	/**
	 * Set the font size measured in sp
	 *
	 * @param fontSize
	 */
	public void setFontSize(int fontSize) {
		mFontSize = Math.min(64, Math.max(6, fontSize));

		// Update views
		WebSettings settings = mQuestion.getSettings();
		settings.setDefaultFontSize(mFontSize);

		settings = mAnswer.getSettings();
		settings.setDefaultFontSize(mFontSize);
	}

	/**
	 * @return the font size measured in px
	 */
	public int getFontSizeInPx() {
		DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mFontSize, metrics);
	}

	public interface OnCardAnsweredListener {
		void onCardAnswered(Card card, boolean correctAnswer);
	}
}
