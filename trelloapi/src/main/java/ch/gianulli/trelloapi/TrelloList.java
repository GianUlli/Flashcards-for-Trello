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

public class TrelloList implements Parcelable {

	public static final Parcelable.Creator<TrelloList> CREATOR
			= new Parcelable.Creator<TrelloList>() {
		public TrelloList createFromParcel(Parcel in) {
			return new TrelloList(in);
		}

		public TrelloList[] newArray(int size) {
			return new TrelloList[size];
		}
	};

	private String mId;

	private Board mBoard;

	private String mName;

	private ArrayList<Card> mCards;

	public TrelloList(String id, Board board, String name, ArrayList<Card> cards) {
		mId = id;
		mBoard = board;
		mName = name;
		mCards = cards;
	}

	@SuppressWarnings("unchecked")
	public TrelloList(Parcel in) {
		this(in.readString(), (Board) in.readParcelable(null), in.readString(), (ArrayList<Card>)
				in.readArrayList(null));
	}

	public static TrelloList getList(TrelloAPI api, Board board, String id)
			throws TrelloNotAuthorizedException, TrelloNotAccessibleException {
		try {
			LinkedHashMap<String, String> args = new LinkedHashMap<>();
			args.put("cards", "open");
			args.put("card_fields", "name,desc");
			JSONObject list = api.makeJSONObjectRequest("GET", "lists/" + id, args, true);
			if (list != null) {
				JSONArray cardArray = list.getJSONArray("cards");
				ArrayList<Card> cards = new ArrayList<>(cardArray.length());
				for (int j = 0; j < cardArray.length(); ++j) {
					JSONObject card = cardArray.getJSONObject(j);
					cards.add(new Card(card.getString("id"), null, card.getString("name"),
							card.getString("desc"))); // list is null for now
				}
				TrelloList trelloList = new TrelloList(list.getString("id"), board, list
						.getString("name"), cards);
				// add list reference to cards
				for (Card c : cards) {
					c.setList(trelloList);
				}

				return trelloList;
			}
		} catch (JSONException e) {
			throw new TrelloNotAccessibleException("Server answer was not correctly formatted.");
		}

		return null;
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

	public ArrayList<Card> getCards() {
		return mCards;
	}

	public void setCards(ArrayList<Card> cards) {
		mCards = cards;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TrelloList) {
			return ((TrelloList) o).getId().equals(mId);
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
		dest.writeParcelable(mBoard, 0);
		dest.writeString(mName);
		dest.writeList(mCards);
	}
}
