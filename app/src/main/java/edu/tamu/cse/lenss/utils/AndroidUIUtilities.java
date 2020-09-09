package edu.tamu.cse.lenss.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class AndroidUIUtilities {

	private AndroidUIUtilities() { }

    public static void showToast(Context context, int id, boolean longToast) {
        ///Toast.makeText(context, id, longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
    
    public static void showToast(Context context, String text, boolean longToast) {
    	Toast.makeText(context, text, longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
    
    public static void showToast(Context context, int id) {
    	showToast(context, id, false);
    }
    
    public static void showToast(Context context, String text) {
    	showToast(context, text, false);
    }
    
    public static void hideSoftKeyboard(Context context){
    	((Activity)context).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    
    public static void hideSoftKeyboard(Context context, View view){
    	InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
