package edu.mines.kkincade.looneytunes.detector;

import edu.mines.kkincade.looneytunes.detector.R;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** When a character is detected, a dialog is placed on the screen with
 * the character's information. This class represents one of those dialogs. **/
public class CharacterDialog {

	protected static final String TAG = null;
	
	final Dialog dialog;
	final WebView web;
	private String address;
	private String name;
	
	@SuppressLint("SetJavaScriptEnabled")
	public CharacterDialog(Context context, String name, String address){
		this.setName(name);
		this.setAddress(address);
		dialog = new Dialog(context);
		dialog.setContentView(R.layout.info_chars);
		dialog.setCancelable(true);
		dialog.setTitle(name);
		web = (WebView) dialog.findViewById(R.id.webView);
		
		web.setWebViewClient(new WebViewClient() {
			public void onPageFinished(WebView view, String url) {
				web.setVisibility(View.VISIBLE);
			}
		});
		web.getSettings().setJavaScriptEnabled(true);
		web.loadUrl(address);
	}
	
	public void show(){
		dialog.show();
	}

	/** ----------------------- Getters and Setters ----------------------- **/
	
	public String getAddress() { return address; }
	public String getName() { return name; }
	public void setAddress(String address) { this.address = address; }
	public void setName(String name) { this.name = name; }
}
