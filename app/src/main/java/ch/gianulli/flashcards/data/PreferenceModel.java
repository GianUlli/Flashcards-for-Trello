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

package ch.gianulli.flashcards.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class that handles all SharedPreferences operations
 */
public class PreferenceModel {

	public static final String KEY_HIDDEN_BOARDS = "hidden_boards";

	public static final String KEY_CARD_AMOUNT_POSITION = "card_amount";

	public static final String KEY_CARD_SELECTION_MODE_POSITION = "selection_mode";

	public static final String KEY_FONT_SIZE = "font_size";

	public static final String KEY_MATHML_ENABLED = "enable_mathml";

	private SharedPreferences mPreferences;

	public PreferenceModel(Context context) {
		mPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext
				());
	}

	/**
	 * @return preferred font size in sp
	 */
	public int getFontSize() {
		return Integer.parseInt(mPreferences.getString(KEY_FONT_SIZE, "20"));
	}

	/**
	 * @param fontSize preferred font size in sp
	 */
	public void setFontSize(int fontSize) {
		mPreferences.edit().putString(KEY_FONT_SIZE, String.valueOf(fontSize)).apply();
	}

	public boolean isMathMLEnabled() {
		return mPreferences.getBoolean(KEY_MATHML_ENABLED, false);
	}

	public void setMathMLEnabled(boolean mathMLEnabled) {
		mPreferences.edit().putBoolean(KEY_MATHML_ENABLED, mathMLEnabled).apply();
	}

	public Set<String> getHiddenBoards() {
		return new HashSet<>(mPreferences.getStringSet(KEY_HIDDEN_BOARDS, new HashSet<String>()));
	}

	public void addHiddenBoards(String... ids) {
		Set<String> hiddenBoards = getHiddenBoards();
		hiddenBoards.addAll(Arrays.asList(ids));
		mPreferences.edit().putStringSet(KEY_HIDDEN_BOARDS, hiddenBoards).apply();
	}

	public void removeHiddenBoard(String id) {
		Set<String> hiddenBoards = getHiddenBoards();
		hiddenBoards.remove(id);
		mPreferences.edit().putStringSet(KEY_HIDDEN_BOARDS, hiddenBoards).apply();
	}

	public void resetHiddenBoards() {
		mPreferences.edit().putStringSet(KEY_HIDDEN_BOARDS, new HashSet<String>()).apply();
	}

	/**
	 * @return last selected position in card amount spinner or 0 if no data has been saved yet
	 */
	public int getCardAmountPosition() {
		return mPreferences.getInt(KEY_CARD_AMOUNT_POSITION, 0);
	}

	/**
	 * Stores last card amount spinner position
	 *
	 * @param position card amount spinner position
	 */
	public void setCardAmountPosition(int position) {
		mPreferences.edit().putInt(KEY_CARD_AMOUNT_POSITION, position).apply();
	}

	/**
	 * @return last selected position in card selection mode spinner or 0 if no data has been
	 * saved yet
	 */
	public int getCardSelectionModePosition() {
		return mPreferences.getInt(KEY_CARD_SELECTION_MODE_POSITION, 0);
	}

	/**
	 * Stores last card selection mode spinner position
	 *
	 * @param position card selection mode spinner position
	 */
	public void setCardSelectionModePosition(int position) {
		mPreferences.edit().putInt(KEY_CARD_SELECTION_MODE_POSITION, position).apply();
	}

}
