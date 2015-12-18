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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;


public class SessionActivity extends AppCompatActivity {

	public static final int MODE_AT_RANDOM = 0;

	public static final int MODE_FROM_TOP = 1;

	public static final int MODE_FROM_BOTTOM = 2;

	public static final String KEY_LIST_ID = "list_id";

	public static final String KEY_TITLE = "title";

	public static final String KEY_MODE = "mode";

	public static final String KEY_NBR_OF_CARDS = "cards";

	public static final String KEY_CORRECT_CARDS_LIST_ID = "correct_cards_list_id";

	public static final String KEY_WRONG_CARDS_LIST_ID = "wrong_cards_list_id";

	private SessionFragment mFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		Bundle extras = getIntent().getExtras();
		if (!extras.containsKey(KEY_LIST_ID)) {
			throw new IllegalArgumentException("No list in extras");
		}
		String listId = extras.getString(KEY_LIST_ID);
		if (!extras.containsKey(KEY_TITLE)) {
			throw new IllegalArgumentException("Title not specified in extras");
		}
		String title = extras.getString(KEY_TITLE);
		if (!extras.containsKey(KEY_MODE)) {
			throw new IllegalArgumentException("Mode not specified in extras");
		}
		int mode = extras.getInt(KEY_MODE);
		if (!extras.containsKey(KEY_NBR_OF_CARDS)) {
			throw new IllegalArgumentException("Number of cards not specified in extras");
		}
		int nbrOfCards = extras.getInt(KEY_NBR_OF_CARDS);
		if (!extras.containsKey(KEY_CORRECT_CARDS_LIST_ID)) {
			throw new IllegalArgumentException("Correct cards list id not specified in extras");
		}
		String correctCardsListId = extras.getString(KEY_CORRECT_CARDS_LIST_ID);
		if (!extras.containsKey(KEY_WRONG_CARDS_LIST_ID)) {
			throw new IllegalArgumentException("Wrong cards list id not specified in extras");
		}
		String wrongCardsListId = extras.getString(KEY_WRONG_CARDS_LIST_ID);

		mFragment = (SessionFragment) getSupportFragmentManager().findFragmentByTag
				("session_fragment");
		if (mFragment == null) {
			mFragment = SessionFragment.newInstance(listId, title, mode, nbrOfCards,
					correctCardsListId, wrongCardsListId);
			getSupportFragmentManager().beginTransaction().add(R.id.container, mFragment,
					"session_fragment").commit();
		}

	}
}
