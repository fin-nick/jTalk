package net.ustyugov.jtalk.view;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.style.ForegroundColorSpan;
import net.ustyugov.jtalk.Colors;
import net.ustyugov.jtalk.listener.TextLinkClickListener;


import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class MyTextView  extends TextView {
	public enum Mode {juick, psto};
	TextLinkClickListener mListener;

	Pattern namePattern = Pattern.compile("((?<=\\A)|(?<=\\s))@[a-z0-9-]*[^\\s\\n\\.,:\\)]*", Pattern.CASE_INSENSITIVE);
	Pattern juickPattern = Pattern.compile("(#[0-9]+(/[0-9]+)?)");
	Pattern pstoPattern = Pattern.compile("(#[\\w]+(/[0-9]+)?)");
	Pattern linkPattern = Pattern.compile("(ht|f)tps?://[a-z0-9\\-\\.]+[a-z]{2,}/?[^\\s\\n]*", Pattern.CASE_INSENSITIVE);

	public MyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setTextWithLinks(SpannableStringBuilder ssb) {
		ArrayList<Hyperlink> list = new ArrayList<Hyperlink>();
		getLinks(list, ssb, linkPattern);

		for (Hyperlink link : list) {
			ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.LINK), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		setText(ssb);
	}
	
	public void setTextWithLinks(SpannableStringBuilder ssb, String nick) { 
		int start = ssb.toString().indexOf(nick);
		if (start >= 0) {
			int end = start + nick.length();
			
			Hyperlink spec = new Hyperlink();
			spec.textSpan = ssb.subSequence(start, end);
			spec.span = new InternalURLSpan(spec.textSpan.toString());
			spec.start = start;
			spec.end = end;
			ssb.setSpan(spec.span, spec.start, spec.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.INBOX_MESSAGE), spec.start, spec.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		ArrayList<Hyperlink> list = new ArrayList<Hyperlink>();
		getLinks(list, ssb, linkPattern);

		for (Hyperlink link : list) {
			ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.LINK), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		}

		setText(ssb);
	}
	
	public void setTextWithLinks(SpannableStringBuilder ssb, Mode mode) {
		ArrayList<Hyperlink> nameList = new ArrayList<Hyperlink>();
		ArrayList<Hyperlink> msgList = new ArrayList<Hyperlink>();
		ArrayList<Hyperlink> linkList = new ArrayList<Hyperlink>();
		if (mode == Mode.juick) getLinks(msgList, ssb, juickPattern);
		else getLinks(msgList, ssb, pstoPattern);
		getLinks(nameList, ssb, namePattern);
		getLinks(linkList, ssb, linkPattern);

		for (Hyperlink link : nameList) {
			ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.setSpan(new StyleSpan(android.graphics.Typeface.SANS_SERIF.getStyle()), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.INBOX_MESSAGE), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		for (Hyperlink link : msgList) {
			ssb.setSpan(new StyleSpan(android.graphics.Typeface.SANS_SERIF.getStyle()), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.LINK), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		for (Hyperlink link : linkList) {
			ssb.setSpan(new StyleSpan(android.graphics.Typeface.SANS_SERIF.getStyle()), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(Colors.LINK), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		setText(ssb);
	}

	public void setOnTextLinkClickListener(TextLinkClickListener newListener) {
		mListener = newListener;
	}

	private final void getLinks(ArrayList<Hyperlink> links, Spannable s, Pattern pattern) {
		Matcher m = pattern.matcher(s);

		while (m.find())
		{
			int start = m.start();
			int end = m.end();
        
			Hyperlink spec = new Hyperlink();

			spec.textSpan = s.subSequence(start, end);
			spec.span = new InternalURLSpan(spec.textSpan.toString());
			spec.start = start;
			spec.end = end;

			links.add(spec);
		}
	}

	public class InternalURLSpan extends ClickableSpan {
		private String clickedSpan;

		public InternalURLSpan (String clickedString) {
			clickedSpan = clickedString;
		}

		@Override
		public void onClick(View textView) {
			mListener.onTextLinkClick(textView, clickedSpan);
		}
	}

	class Hyperlink {
		CharSequence textSpan;
		InternalURLSpan span;
		int start;
		int end;
	}
}
