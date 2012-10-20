package net.ustyugov.jtalk.view;

import android.content.Context;
import android.widget.EditText;

public class MyEditText extends EditText {
	private String type;
	private String var;

	public MyEditText(Context context) {
		super(context);
	}
	
	public void setType(String type) { this.type = type; }
	public void setVar(String var) { this.var = var; }
	public String getType() { return this.type; }
	public String getVar() { return this.var; }
}
