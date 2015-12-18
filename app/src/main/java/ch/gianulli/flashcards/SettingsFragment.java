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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import ch.gianulli.flashcards.data.PreferenceModel;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsFragment extends PreferenceFragment implements
		SharedPreferences.OnSharedPreferenceChangeListener {

	private PreferenceModel mPrefModel;

	public SettingsFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPrefModel = new PreferenceModel(getActivity());

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.pref_general);

		// Set listener
		getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener
				(this);

		// Reset hidden boards on click
		Preference prefResetHiddenBoard = findPreference("reset_hidden_boards");
		prefResetHiddenBoard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {


			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(R.string.reset_hidden_boards_are_you_sure);
				builder.setPositiveButton(R.string.reset, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						mPrefModel.resetHiddenBoards();
						dialogInterface.dismiss();
					}
				});
				builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.dismiss();
					}
				});
				builder.show();

				return true;
			}
		});

		setRetainInstance(true);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO: implement this...
	}
}
