package net.ustyugov.jtalk;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Holders {
	
	public static class GroupHolder {
		public TextView text;
		public TextView counter;
		public ImageView state;
		public ImageView messageIcon;
	}

    public static class AccountHolder {
        public TextView jid;
        public TextView status;
        public ImageView state;
        public ImageView avatar;
    }
	
	public static class ItemHolder {
		public TextView name;
		public TextView status;
		public TextView counter;
		public ImageView statusIcon;
		public ImageView messageIcon;
		public ImageView caps;
		public ImageView avatar;
	}

}
