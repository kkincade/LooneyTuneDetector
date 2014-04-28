package edu.mines.kkincade.looneytunes.extended;

import java.util.ArrayList;

import edu.mines.kkincade.looneytunes.detector.ObjectList;
import edu.mines.kkincade.looneytunes.detector.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AdapterTwoLinesImage extends BaseAdapter {
	
	// Variables
	public ArrayList<ObjectList> personal;
	Context context;
	
	// Constructor
	public AdapterTwoLinesImage(Context context, ArrayList<ObjectList> data) {
		this.context = context;
		personal = data;
	}

	
	/** ---------------------------- Getters and Setters -------------------------------- **/
	
	@Override
	public int getCount() { return personal.size();	}
	
	@Override
	public Object getItem(int arg0) { return personal.get(arg0); }
	
	@Override
	public long getItemId(int arg0) { return arg0; }

	public View getView(int index, View v, ViewGroup parent) {
		View view = v;
		
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.adapter_img_two_lines, null);
		}
		
		ObjectList objectList = personal.get(index);
		ImageView i = (ImageView) view.findViewById(R.id.adapter_icon);
		TextView t = (TextView) view.findViewById(R.id.adapter_title);
		TextView m = (TextView) view.findViewById(R.id.adapter_message);
		
		i.setImageResource(objectList.getIcon());
		t.setText(objectList.getTitle());
		m.setText(objectList.getMsg());

		return view;
	}

   
}


