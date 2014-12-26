package com.example.speechrecognizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.provider.ContactsContract;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Contacts.Phones;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.Settings;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.View;


public class MainActivity extends Activity implements RecognitionListener{
	SpeechRecognizer recognizer;
	public DevicePolicyManager mDPM;
	ComponentName devAdminReceiver;
	List<ResolveInfo> pkgAppsList;
	

	public static class MyAdmin extends DeviceAdminReceiver {
		// implement onEnabled(), onDisabled(), …
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//Setting up Speech Recognizer
		recognizer = SpeechRecognizer.createSpeechRecognizer(this);
		recognizer.setRecognitionListener(this);

		//Getting Admin Rights to lock the phone
		mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
		devAdminReceiver = new ComponentName(this, MyAdmin.class);
		if (!mDPM.isAdminActive(devAdminReceiver)) {

			Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, devAdminReceiver);
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"Make me admin now!"); 
			startActivityForResult(intent,  1);
		}

		//Get the list of installed apps on phone
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		pkgAppsList = this.getPackageManager().queryIntentActivities( mainIntent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 ) {
			if (resultCode == Activity.RESULT_OK) {
				Log.d("Speech", "Has became the Device Admin");
			}
			else
			{
				Log.d("Speech", "Admin request canceled");
			}
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	public void onStartSpeech(View view){
		Intent recIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		recIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		recIntent.putExtra("calling_package","yourcallingpackage");
		recognizer.startListening(recIntent);

	}

	public void turnGPSOn()
	{
		Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
		intent.putExtra("enabled", true);
		sendBroadcast(intent);

		String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		if(!provider.contains("gps")){ //if gps is disabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider"); 
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3")); 
			sendBroadcast(poke);


		}
	}
	// automatic turn off the gps
	public void turnGPSOff()
	{
		String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		if(provider.contains("gps")){ //if gps is enabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3")); 
			sendBroadcast(poke);
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onBeginningOfSpeech() {
		Log.d("Speech", "onBeginningOfSpeech");
	}

	@Override
	public void onBufferReceived(byte[] buffer) {
		Log.d("Speech", "onBufferReceived");
	}

	@Override
	public void onEndOfSpeech() {
		Log.d("Speech", "onEndOfSpeech");
	}

	@Override
	public void onError(int error) {
		Log.d("Speech", "onError");
	}

	@Override
	public void onEvent(int eventType, Bundle params) {
		Log.d("Speech", "onEvent");
	}

	@Override
	public void onPartialResults(Bundle partialResults) {
		Log.d("Speech", "onPartialResults");
	}

	@Override
	public void onReadyForSpeech(Bundle params) {
		Log.d("Speech", "onReadyForSpeech");
	}


	@Override
	public void onResults(Bundle results) {
		Log.d("Speech", "results");

		if(results != null)
		{
			ArrayList<String> li_results = results.getStringArrayList("results_recognition");
			while(!li_results.isEmpty())
			{
				String str = li_results.remove(0);
				Log.d("Speech",str);
				Iterator<ResolveInfo> itr = pkgAppsList.iterator();
				while(itr.hasNext())
				{
					ResolveInfo rf = itr.next(); 
					String appName = rf.loadLabel(this.getPackageManager()).toString();
					//Log.d("Speech",appName);

					if(appName.equalsIgnoreCase(str))
					{
						Log.d("Speech",str + " Matches with " +appName);
						//Log.d("Speech",rf.resolvePackageName);

						ActivityInfo activity=rf.activityInfo;
						ComponentName name=new ComponentName(activity.applicationInfo.packageName,activity.name);
						Intent i=new Intent(Intent.ACTION_MAIN);

						i.addCategory(Intent.CATEGORY_LAUNCHER);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
								Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
						i.setComponent(name);

						startActivity(i);

						//Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(rf.resolvePackageName);

						//startActivity(LaunchIntent);
						return;
					}
					if(str.equalsIgnoreCase("lock"))
					{
						LockNow();
						return;
					}
					if(str.equalsIgnoreCase("GPS"))
					{
						Intent i=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						//i.addCategory(Intent.CATEGORY_LAUNCHER);
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
								Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
						startActivity(i);
						return;
					}
					WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
					if(str.equalsIgnoreCase("wifi on") && !wifiManager.isWifiEnabled())
					{

						wifiManager.setWifiEnabled(true);
						//wifiManager.setWifiEnabled(false);
						return;
					}
					else if(str.equalsIgnoreCase("wifi off"))
					{
						//WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
						//wifiManager.setWifiEnabled(true);
						wifiManager.setWifiEnabled(false);
						return;
					}
					String[] words = str.split(" ");
					if(words[0].equalsIgnoreCase("Call"))
					{
						String contactName = "";
						for(int i =1; i<words.length ; i++)
						{
							//Log.i("Speech",words[i]);
							contactName = contactName + words[i] + " ";
						}
						contactName.trim().toLowerCase();
						ContentResolver cr = getContentResolver();
						//Query all the Content URI for the contact name
						Log.i("Speech","Goint to query this name: " +contactName);
						//Cursor cur = cr.query(Contacts.CONTENT_URI,null,"lower(DISPLAY_NAME) = '" + contactName + "'",null,null);
						Cursor cur = cr.query(Contacts.CONTENT_URI,null,"lower(display_name)=lower('" + contactName + "')",null,null);
						Log.i("Speech",cur.toString());
						if (cur.getCount() > 0) {
							if (cur.moveToFirst()) {
								String id = cur.getString(cur.getColumnIndex(Contacts._ID));
								//String name = cur.getString(cur.getColumnIndex(Contacts.DISPLAY_NAME));
								if (Integer.parseInt(cur.getString(cur.getColumnIndex(Contacts.HAS_PHONE_NUMBER))) > 0) {
									//Query the content URI matching the particular contact ID, 
									//I am using here the first contact matched
									Cursor pCur = cr.query(Phone.CONTENT_URI,null, Phone.CONTACT_ID + " = " + id,null,null); 
									if (pCur.moveToFirst()) {
										String number = pCur.getString(pCur.getColumnIndex(Phone.NUMBER));
										
										Intent callIntent = new Intent(Intent.ACTION_CALL);
										callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
												Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
										callIntent.setData(Uri.parse("tel:"+number));
										startActivity(callIntent);
									} 
									pCur.close();
								}
							}
						}
						return;
					}
				}
			}
		}
	}

	@Override
	public void onRmsChanged(float rmsdB) {
		//Log.d("Speech", "onRmsChanged");
	}

	public void LockNow(){
		boolean admin = mDPM.isAdminActive(devAdminReceiver);
		if (admin)
			mDPM.lockNow();
		else
			Log.d("Speech","Not an Admin");
	}

}
