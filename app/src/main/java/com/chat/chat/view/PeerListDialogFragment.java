package com.chat.chat.view;

import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.chat.chat.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * PeerListDialogFragment.java
 *
 */
public class PeerListDialogFragment extends DialogFragment
		implements AdapterView.OnItemClickListener {

	public interface PeerListDialogFragmentListener {
		void onItemClick(String item);
	}

	private ListView _lvList;

	private PeerListDialogFragmentListener	_listener;
	private ArrayList<String> _items = new ArrayList<>();
	private HashMap<String, String> _itemsHash;


	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Window window = getDialog().getWindow();
		window.requestFeature(Window.FEATURE_NO_TITLE);

		Context context = inflater.getContext();
		WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point ptSize = new Point();
		display.getSize(ptSize);

		window.setLayout(ptSize.x * 2 / 3, ptSize.y * 2 / 3);

		View vwDialog = inflater.inflate(R.layout.fragment_dialog_peerlist, container, false);
		_lvList = vwDialog.findViewById(R.id.listView);
		_lvList.setOnItemClickListener(this);

		return vwDialog;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, _items);
		_lvList.setAdapter(adapter);
	}

	@Override
	public void onDestroyView()	{
		_listener = null;
		_lvList = null;
		_items = null;

		super.onDestroyView();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (null != _listener)	{
			String item = _items.get(position);
			String userId = _itemsHash.get(item);
			_listener.onItemClick(userId);
		}

		dismiss();
	}

	public void setListener(PeerListDialogFragmentListener listener)
	{
		_listener = listener;
	}

	public void setItems(HashMap<String, String> hashMap) {
		_items.clear();
		_itemsHash = hashMap;
		Iterator it = _itemsHash.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			System.out.println(pair.getKey() + " = " + pair.getValue());
			_items.add(pair.getKey().toString());
			//it.remove(); // avoids a ConcurrentModificationException
		}
	}
}