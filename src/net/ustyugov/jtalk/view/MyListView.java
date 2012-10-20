package net.ustyugov.jtalk.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ListView;

public class MyListView extends ListView {
	private boolean scroll = true;

	public MyListView(Context context) {
		super(context);
	}
	
	public MyListView(Context context, AttributeSet attr) {
		super(context, attr);
	}
	
	public void setScroll(boolean scroll) {
		this.scroll = scroll;
	}
	
	public boolean isScroll() {
		return scroll;
	}
	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (scroll) {
			if (Build.VERSION.SDK_INT >= 8) smoothScrollToPosition(getCount());
        	else setSelection(getCount());
		}
	}
}
