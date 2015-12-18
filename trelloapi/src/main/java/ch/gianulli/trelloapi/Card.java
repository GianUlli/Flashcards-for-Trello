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

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.LinkedHashMap;

public class Card implements Parcelable {

	public static final Parcelable.Creator<Card> CREATOR
			= new Parcelable.Creator<Card>() {
		public Card createFromParcel(Parcel in) {
			return new Card(in);
		}

		public Card[] newArray(int size) {
			return new Card[size];
		}
	};

	private String mId;

	private TrelloList mList;

	private String mName;

	private String mDesc;

	public Card(String id, TrelloList list, String name, String desc) {
		mId = id;
		mList = list;
		mName = name;
		mDesc = desc;
	}

	private Card(Parcel in) {
		this(in.readString(), (TrelloList) in.readParcelable(null), in.readString(), in.readString
				());
	}

	/**
	 * Moves this card to a different list. Attention: this operation does not update TrelloList
	 * objects with a reference to this card.
	 * <p/>
	 * This operation is asynchronous and handles errors itself.
	 *
	 * @param api
	 * @param listId
	 */
	public void moveToListAsync(final TrelloAPI api, final String listId) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... voids) {
				try {
					LinkedHashMap<String, String> args = new LinkedHashMap<>();
					args.put("value", listId);
					JSONObject result = api.makeJSONObjectRequest("PUT", "cards/" + mId +
							"/idList", args, true);
					if (result == null) {
						Log.i("TrelloAPI", "Error occurred when moving card: null result");
						return false;
					}
				} catch (TrelloNotAccessibleException | TrelloNotAuthorizedException e) {
					Log.i("TrelloAPI", "Error occurred when moving card: ", e);
					return false;
				}

				return true;
			}

			@Override
			protected void onPostExecute(Boolean successful) {
				if (!successful) {
					Toast.makeText(api.getContext(), "Error: Card could not be moved.", Toast
							.LENGTH_LONG).show();
				}
			}
		}.execute();
	}

	public String getId() {
		return mId;
	}

	public void setId(String id) {
		mId = id;
	}

	public TrelloList getList() {
		return mList;
	}

	public void setList(TrelloList list) {
		mList = list;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	public String getDesc() {
		return mDesc;
	}

	public void setDesc(String desc) {
		mDesc = desc;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Card) {
			return ((Card) o).getId().equals(mId);
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
		dest.writeParcelable(mList, 0);
		dest.writeString(mName);
		dest.writeString(mDesc);
	}
}
