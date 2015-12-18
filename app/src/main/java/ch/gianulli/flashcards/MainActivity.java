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
import android.util.Log;
import android.widget.Toast;

import ch.gianulli.trelloapi.Board;
import ch.gianulli.trelloapi.TrelloList;


public class MainActivity extends AppCompatActivity implements BoardsFragment
		.BoardsFragmentListener, ListsFragment.ListsFragmentListener {

	private BoardsFragment mBoardsFragment;

	private ListsFragment mListsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mBoardsFragment = (BoardsFragment) getSupportFragmentManager().findFragmentByTag
				("boards_fragment");
		if (mBoardsFragment == null) {
			mBoardsFragment = BoardsFragment.newInstance();
			getSupportFragmentManager().beginTransaction().add(R.id.container, mBoardsFragment,
					"boards_fragment").commit();
		} else {
			mBoardsFragment.setListener(this);
		}

		mListsFragment = (ListsFragment) getSupportFragmentManager().findFragmentByTag
				("lists_fragment");
		if (mListsFragment != null) {
			mListsFragment.setListener(this);
		}

	}

	@Override
	public void onBoardSelected(Board board) {
		Log.d("Flashcards for Trello", "Selected board with id " + board.getId());

		mListsFragment = ListsFragment.newInstance(board);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, mListsFragment,
				"lists_fragment").addToBackStack("lists_fragment").commit();
	}

	@Override
	public void onListSelected(TrelloList list) {
		Log.d("Flashcards for Trello", "Selected list with id " + list.getId());
		Toast.makeText(this, "Selected list with id " + list.getId(), Toast.LENGTH_SHORT).show();
	}
}
