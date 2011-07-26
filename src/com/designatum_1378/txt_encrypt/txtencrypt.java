package com.designatum_1378.txt_encrypt;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.EditText;
import android.app.Dialog;
import android.util.Log;
import android.content.pm.PackageInfo;
import android.text.util.Linkify;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;

//For menu
import android.view.Menu;
import android.view.MenuInflater;
import android.content.DialogInterface;
import android.view.MenuItem;

//For export
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.app.AlertDialog;

//For import
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.String;

//For encrypt/decrypt
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

//For preference
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

//For Kik
import com.kik.platform.KikClient;
import com.kik.platform.KikData;
import com.kik.platform.KikMessage;

public class txtencrypt extends Activity
{
    private String origString, encryptString, enString, decrypted, getInput;
	private TextView tv, tv2;
	private EditText et, et2;
	final Activity activity = this;
	private SharedPreferences sharedPrefs;
	private SharedPreferences.Editor editor;
	
	private String extStoDir = Environment.getExternalStorageDirectory().toString() + "/TxTEncrypt";
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		//Will check if there's a preference item to choose how to export the message
		if(! sharedPrefs.contains("export"))
		{
			editor = sharedPrefs.edit(); 
			editor.putString("export", "kik"); 
			editor.commit(); 
		}
		
		//Presents the EULA dialog only once (during first boot)
		if(!sharedPrefs.getBoolean("EULA", false))
			presentEULA();
			
		
		//Sets up first text box
		tv = (TextView) findViewById(R.id.textView);
		tv.setText("Enter the message you want to send: ");
		et = (EditText) findViewById(R.id.textBox);
		
		//Sets up second text box		
		tv2 = (TextView) findViewById(R.id.textView2);
		tv2.setText("Any decrypted text goes here: ");
		et2 = (EditText) findViewById(R.id.textBox2);
		
		//Sets up button to encrypt message in 'et' using default key
		Button btnEncrypt = (Button) findViewById(R.id.okayButton);
		btnEncrypt.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				origString = et.getText().toString();
				
				//Will first encrypt the string before deciding what to do with it
				try
				{
					enString = txtencrypt.encrypt("hello", origString);
				}
				catch(Exception e)
				{}
				
				if(sharedPrefs.getString("export", "HERPADERP").equals("kik"))
					kikTxt();
				else
					emailTxt();
				
			}
		});
		
		//Sets up button to encrypt message in 'et' using a user-defined key
		Button btnPass = (Button) findViewById(R.id.wPassButton);
		btnPass.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				//Sets up dialog prompting for a key
				final Dialog dialog = new Dialog(txtencrypt.this);
				dialog.setContentView(R.menu.maindialog);
				dialog.setTitle("Creating custom key");
				
				TextView key_text = (TextView) dialog.findViewById(R.id.key_label);
				final EditText key_input = (EditText) dialog.findViewById(R.id.key_entry);
				
				Button cancel_button = (Button) dialog.findViewById(R.id.cancelButton);
				cancel_button.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						dialog.dismiss();
					}
				});
				
				//Takes the user-inputted key and set that as the key for the encryption
				Button okay_button = (Button) dialog.findViewById(R.id.okayButton);
				okay_button.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						String key_name = key_input.getText().toString();
						if(key_name.compareTo("") == 0)
							key_name = "hello";
						
						origString = et.getText().toString();
				
						try
						{ enString = txtencrypt.encrypt(key_name, origString); }
						catch(Exception e)
						{}
						
						if(sharedPrefs.getString("export", "HERPADERP").equals("kik"))
							kikTxt();
						else
							emailTxt();		
						
						dialog.dismiss();
					}
				});
				
				dialog.show();
			}
		});

		//When pressed, will clear all text in the second text box.
		Button btnClear = (Button) findViewById(R.id.clearDecrypt);
		btnClear.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v)
			{
				et2.setText(null);
			}
		});
		
		//If the app is opened via an intent, 
		Intent intent = getIntent();
		if (intent.getData() != null)
		{
			//Check to see if the app was open via Kik
			if(intent.getScheme().compareTo("kik-com.designatum1393.txtencrypt") == 0){
				try{
					KikData data = KikClient.getDataFromIntent(getIntent());
					KikMessage input = data.getMessage();
					getInput = input.getText();
					decrypted = null;
				} catch(Exception e) {}
			}
			else
			{
				//If not, it will assume it was open via an email attachment
				try{
					InputStream attachment = getContentResolver().openInputStream(intent.getData());
					InputStreamReader reader = new InputStreamReader(attachment);
					char[] fileRead = new char[attachment.available()];
					reader.read(fileRead, 0, attachment.available()); 
					getInput = String.valueOf(fileRead);
					attachment.close();
				} catch(Exception e) {}	
			}
			
			//Dialog will show up prompting user for the key. If there is no key, it will default to the app's key
			final Dialog dialog = new Dialog(txtencrypt.this);
			dialog.setContentView(R.menu.decryptdialog);
			dialog.setTitle("Decrypting...");
			dialog.setCancelable(true);
			
			TextView key_text = (TextView) dialog.findViewById(R.id.key_label);
			final EditText key_input = (EditText) dialog.findViewById(R.id.key_entry);
			
			Button okay_button = (Button) dialog.findViewById(R.id.okayButton);
			okay_button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					String key_name = key_input.getText().toString();
					if(key_name.compareTo("") == 0)
						key_name = "hello";
					
					try{
					decrypted = decrypt(key_name, getInput);
					}
					catch(Exception e)
					{}
					
					et2.setText(decrypted);
					
					dialog.dismiss();
				}
			});
				
			dialog.show();
		}
    }
	
	public void presentEULA()
	{
		AlertDialog.Builder ed = new AlertDialog.Builder(this);
		ed.setIcon(R.drawable.icon);
		ed.setTitle("End-User License Agreement");
		ed.setView(LayoutInflater.from(this).inflate(R.layout.eula_dialog,null));

		ed.setPositiveButton("Agree", 
		new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
			SharedPreferences.Editor editor = sharedPrefs.edit(); 
			editor.putBoolean("EULA", true); 
			editor.commit(); 
			}
		});
		
		ed.setNegativeButton("Disagree (You will be kicked out)", 
		new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
			SharedPreferences.Editor editor = sharedPrefs.edit(); 
			editor.putBoolean("EULA", false);
			editor.commit(); 
			finish();
			}
		});
		ed.show();
	}

	//Inflates menu.xml to create the menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	
	//Chooses what to do when a menu item is selected
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.choose_export:
				exportDialog();
				return true;
			case R.id.about:
				aboutDialog();
				return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	//Creates the "About" dialog when selected
	public void aboutDialog(){
		final TextView message = new TextView(this);
		
		try{
			PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
			String versionInfo = pInfo.versionName;

			String versionString = String.format("Version: %s", versionInfo);
			String authors = "Authors: Vincent Tran";
			String website = "Visit our website: http://1393Designatum.com";
			String cp = "\u00A92011 1393 Designatum, All Rights Reserved.";

			message.setPadding(10, 10, 10, 10);
			message.setText(versionString + "\n\n" + authors + "\n\n" + website + "\n\n" +	cp);
			Linkify.addLinks(message, Linkify.EMAIL_ADDRESSES);
			Linkify.addLinks(message, Linkify.WEB_URLS);
			
			AlertDialog.Builder ad = new AlertDialog.Builder(this);
			ad.setIcon(R.drawable.icon);
			ad.setTitle("About");
			ad.setView(message);

			ad.setPositiveButton("That's nice", 
			new android.content.DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int arg1) {
				}
			});
			ad.show();
			
		}catch(Exception e){}

	}
	
	
	public void exportDialog()
	{
		final CharSequence[] choices = {"Kik Messenger", "Email Attachment"};
		
		int exportChoice = 0;
		
		if(sharedPrefs.getString("export", "HERPADERP").equals("email"))
			exportChoice = 1;
			
		AlertDialog.Builder exporter = new AlertDialog.Builder(this);
		exporter.setIcon(R.drawable.icon);
		exporter.setTitle("Choose how to send the message:");
		exporter.setCancelable(true);
		exporter.setSingleChoiceItems(choices, exportChoice, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				switch(item){
					case 0:
						editor = sharedPrefs.edit(); 
						editor.putString("export", "kik"); 
						editor.commit(); 
					break;
					case 1:
						editor = sharedPrefs.edit(); 
						editor.putString("export", "email"); 
						editor.commit(); 
					break;
				}
			}
		});
		exporter.show();
	}
	
	//Will create the Kik message and send it to Kik Messenger
	private void kikTxt()
	{
		KikMessage message = new KikMessage("com.designatum1393.txtencrypt");
		message.setAndroidDownloadUri("market://details?id=com.designatum_1378.txt_encrypt");
		message.setFallbackUri("http://1393Designatum.com/txt_encrypt.html");
		message.setTitle("Message: ");
		message.setText(enString);
		KikClient.sendMessage(activity, message);
	
	}
	
	//Initializes the .txtencrypt file and stores it into the sdcard temporarily then creates an email intent to send as an attachment
	private void emailTxt()
	{
		try{
			File root = new File(extStoDir);

			if(!root.exists())
				root.mkdirs();
			if(root.canWrite()){				
				File outTXT = new File(extStoDir, "message.txtencrypt");
				
				FileWriter fio = new FileWriter(outTXT);
				fio.write(enString);
				fio.close();
				
				Intent send = new Intent(Intent.ACTION_SEND);
				send.setType("text/txtencrypt");
				send.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/TxTEncrypt/message.txtencrypt"));
				send.putExtra(Intent.EXTRA_SUBJECT, "A Message...");
				startActivity(Intent.createChooser(send, "Send message using..."));
				
				outTXT.deleteOnExit();
				
			}
			else
				Toast.makeText(getApplicationContext(), "App isn't allowed to write to SD! :(",Toast.LENGTH_SHORT).show();
		}catch(IOException e){}	
	}
	
	// Following code taken from: http://www.androidsnippets.com/encryptdecrypt-strings
	
	public static String encrypt(String seed, String cleartext) throws Exception 
	{
		byte[] rawKey = getRawKey(seed.getBytes());
		byte[] result = encrypt(rawKey, cleartext.getBytes());
		return toHex(result);
	}
	
	public static String decrypt(String seed, String encrypted) throws Exception 
	{
		byte[] rawKey = getRawKey(seed.getBytes());
		byte[] enc = toByte(encrypted);
		byte[] result = decrypt(rawKey, enc);
		return new String(result);
	}
	
	private static byte[] getRawKey(byte[] seed) throws Exception 
	{
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		sr.setSeed(seed);
		kgen.init(128, sr); 
		SecretKey skey = kgen.generateKey();
		byte[] raw = skey.getEncoded();
		return raw;
	}
	
	private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception 
	{
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		byte[] encrypted = cipher.doFinal(clear);
		return encrypted;
	}
	
	private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] decrypted = cipher.doFinal(encrypted);
		return decrypted;
	}
	
	public static String toHex(String txt) 
	{
		return toHex(txt.getBytes());
	}
	
	public static String fromHex(String hex) {
		return new String(toByte(hex));
	}
	
	public static String toHex(byte[] buf) 
	{
		if (buf == null)
			return "";
		StringBuffer result = new StringBuffer(2*buf.length);
		for (int i = 0; i < buf.length; i++) {
				appendHex(result, buf[i]);
		}
		return result.toString();
	}
	
	public static byte[] toByte(String hexString) {
		int len = hexString.length()/2;
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++)
			result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
		return result;
	}
	
	private final static String HEX = "0123456789ABCDEF";
	
	private static void appendHex(StringBuffer sb, byte b) 
	{
		sb.append(HEX.charAt((b>>4)&0x0f)).append(HEX.charAt(b&0x0f));
	}
	
	//End code snippet
}
