/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gian Ulli (gian.ulli@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ch.gianulli.trelloapi;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Board implements Parcelable {

	public static final Parcelable.Creator<Board> CREATOR
			= new Parcelable.Creator<Board>() {
		public Board createFromParcel(Parcel in) {
			return new Board(in);
		}

		public Board[] newArray(int size) {
			return new Board[size];
		}
	};

	private static final int STANDARD_COLOR = 0xFF0079BF; // blue

	private String mId;

	private String mName;

	private int mColor;

	protected Board(String id, String name) {
		mId = id;
		mName = name;
		mColor = STANDARD_COLOR;
	}

	protected Board(String id, String name, String color) {
		mId = id;
		mName = name;
		setColorHex(color);
	}

	protected Board(String id, String name, int color) {
		mId = id;
		mName = name;
		setColor(color);
	}

	private Board(Parcel in) {
		this(in.readString(), in.readString(), in.readInt());
	}

	/**
	 * Returns a list of all boards of the user. Attention: this method makes a synchronous
	 * network request!
	 *
	 * @param api
	 * @return list of boards
	 * @throws TrelloNotAccessibleException if connection is broken
	 * @throws TrelloNotAuthorizedException if token is not valid
	 */
	public static ArrayList<Board> listAllBoards(TrelloAPI api) throws
			TrelloNotAccessibleException, TrelloNotAuthorizedException {

		ArrayList<Board> result = null;

		try {
			JSONArray array = api.makeJSONArrayRequest("GET", "members/me/boards", null, true);
			if (array != null) {
				result = new ArrayList<>(array.length());

				for (int i = 0; i < array.length(); ++i) {
					JSONObject board = array.getJSONObject(i);

					// We only have a color
					if (board.getJSONObject("prefs").getString("backgroundImage").equals("null")) {
						result.add(new Board(board.getString("id"), board.getString("name"), board
								.getJSONObject("prefs").getString("backgroundColor")));
					} else { // background image --> use standard color
						result.add(new Board(board.getString("id"), board.getString("name"),
								STANDARD_COLOR));
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			throw new TrelloNotAccessibleException("Server answer was not correctly formatted.");
		}

		if (result == null) {
			throw new TrelloNotAccessibleException("Server answer was null.");
		}

		return result;
	}

	public ArrayList<TrelloList> getAllLists(TrelloAPI api) throws
			TrelloNotAuthorizedException, TrelloNotAccessibleException {
		ArrayList<TrelloList> result = new ArrayList<>();

		try {
			LinkedHashMap<String, String> args = new LinkedHashMap<>();
			args.put("cards", "open");
			args.put("card_fields", "name,desc");
			JSONArray array = api.makeJSONArrayRequest("GET", "boards/" + mId + "/lists", args,
					true);
			if (array != null) {
				for (int i = 0; i < array.length(); ++i) {
					JSONObject list = array.getJSONObject(i);
					JSONArray cardArray = list.getJSONArray("cards");
					ArrayList<Card> cards = new ArrayList<>(cardArray.length());
					for (int j = 0; j < cardArray.length(); ++j) {
						JSONObject card = cardArray.getJSONObject(j);
						cards.add(new Card(card.getString("id"), null, card.getString("name"),
								card.getString("desc"))); // list is null for now
					}
					TrelloList trelloList = new TrelloList(list.getString("id"), this, list
							.getString("name"), cards);
					// add list reference to cards
					for (Card c : cards) {
						c.setList(trelloList);
					}
					result.add(trelloList);
				}
			}

		} catch (JSONException e) {
			throw new TrelloNotAccessibleException("Server answer was not correctly formatted.");
		}

		return result;
	}

	public String getId() {
		return mId;
	}

	public void setId(String id) {
		mId = id;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	public int getColor() {
		return mColor;
	}

	public void setColor(int color) {
		mColor = color;
	}

	/**
	 * Parses a color string
	 *
	 * @param hex format: '#rrggbb'
	 */
	public void setColorHex(String hex) {
		if (!hex.matches("#[0-9abcdefABCDEF]{6}")) {
			mColor = STANDARD_COLOR;
		} else {
			hex = "FF" + hex.substring(1, 7);
			mColor = (int) Long.parseLong(hex, 16);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Board) {
			return ((Board) o).getId().equals(mId);
		}
		return super.equals(o);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mId);
		dest.writeString(mName);
		dest.writeInt(mColor);
	}
}
