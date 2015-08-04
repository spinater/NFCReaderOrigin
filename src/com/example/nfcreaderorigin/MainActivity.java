/*
 * Copyright (C) 2011-2013 Advanced Card Systems Ltd. All Rights Reserved.
 * 
 * This software is the confidential and proprietary information of Advanced
 * Card Systems Ltd. ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with ACS.
 */

package com.example.nfcreaderorigin;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony.Sms.Conversations;
import android.support.v4.app.TaskStackBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.LineHeightSpan.WithDensity;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.*;


import com.acs.smartcard.Features;
import com.acs.smartcard.PinModify;
import com.acs.smartcard.PinProperties;
import com.acs.smartcard.PinVerify;
import com.acs.smartcard.ReadKeyOption;
import com.acs.smartcard.Reader;
import com.acs.smartcard.Reader.OnStateChangeListener;
import com.acs.smartcard.TlvProperties;

/**
 * Test program for ACS smart card readers.
 * 
 * @author Godfrey Chung
 * @version 1.1.1, 16 Apr 2013
 */
public class MainActivity extends Activity {

	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

	private static final String[] stateStrings = { "Unknown", "Absent",
		"Present", "Swallowed", "Powered", "Negotiable", "Specific" };

	private static final String[] featureStrings = { "FEATURE_UNKNOWN",
		"FEATURE_VERIFY_PIN_START", "FEATURE_VERIFY_PIN_FINISH",
		"FEATURE_MODIFY_PIN_START", "FEATURE_MODIFY_PIN_FINISH",
		"FEATURE_GET_KEY_PRESSED", "FEATURE_VERIFY_PIN_DIRECT",
		"FEATURE_MODIFY_PIN_DIRECT", "FEATURE_MCT_READER_DIRECT",
		"FEATURE_MCT_UNIVERSAL", "FEATURE_IFD_PIN_PROPERTIES",
		"FEATURE_ABORT", "FEATURE_SET_SPE_MESSAGE",
		"FEATURE_VERIFY_PIN_DIRECT_APP_ID",
		"FEATURE_MODIFY_PIN_DIRECT_APP_ID", "FEATURE_WRITE_DISPLAY",
		"FEATURE_GET_KEY", "FEATURE_IFD_DISPLAY_PROPERTIES",
		"FEATURE_GET_TLV_PROPERTIES", "FEATURE_CCID_ESC_COMMAND" };

	private static final String[] propertyStrings = { "Unknown", "wLcdLayout",
		"bEntryValidationCondition", "bTimeOut2", "wLcdMaxCharacters",
		"wLcdMaxLines", "bMinPINSize", "bMaxPINSize", "sFirmwareID",
		"bPPDUSupport", "dwMaxAPDUDataSize", "wIdVendor", "wIdProduct" };

	private UsbManager mManager;
	private Reader mReader;
	private PendingIntent mPermissionIntent;

	private boolean mAuthenticateRead = false;
	private boolean mReadMode = true;
	private boolean mWriteMode = false;

	private boolean mTaskStart = false;
	private boolean mTaskWriteStart = false;
	private boolean mTaskReadStart = false;

	private static final int MAX_LINES = 25;
	private TextView mResponseTextView;
	private TextView mTextMode;
	private TextView mTextBalance;
	private TextView mTextId;
	private TextView mTextType;
	private TextView mReadTime;
	private TextView mWriteTime;
	private TextView mEndTime;


	private boolean mBlockAuthen = true;
	private boolean mBlockRead = false ;
	private boolean mBlockWrite = false;
	private boolean mOnSetDevice = true;
	private boolean mOnSetDevice2 = false;

	private long initRead = 0;
	private long initWrite = 0;
	private long initStart = 0;
	private long nowRead; 
	private long nowWrite ;
	private long nowStart;

	private long timeEnd,TimeRead,TimeWrite;

	private EditText mWriteDataBalanceEditText;
	private EditText mWriteDataIdEditText;
	private EditText mQty;
	private EditText mWriteType;

	private ArrayAdapter<String> mReaderAdapter;

	private String mReaderAdapterX;
	private String mResponseHex;
	private String mID = "";
	private String mBalance;

	private String mCommand;

	private NumberFormat formatter;
	private SimpleDateFormat sdf;

	private Button mOpenButton;
	private Button mCloseButton;
	//	private Button mPowerButton;
	//	private Button mControlButton;
	private Button mReadData;
	private Button mWriteData;

	private int mBlockCursur;
	private float mPoint = 0;

	//	private Features mFeatures = new Features();
	private PinVerify mPinVerify = new PinVerify();
	private PinModify mPinModify = new PinModify();
	private ReadKeyOption mReadKeyOption = new ReadKeyOption();
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();

			if (ACTION_USB_PERMISSION.equals(action)) {

				synchronized (this) {

					UsbDevice device = (UsbDevice) intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

						if (device != null) {

							// Open reader
							logMsg("Opening reader: " + device.getDeviceName()
									+ "...");

							new OpenTask().execute(device);

						}

					} else {

						logMsg("Permission denied for device "
								+ device.getDeviceName());

						// Enable open button
						mOpenButton.setEnabled(true);
					}
				}

			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

				synchronized (this) {

					// Update reader list
					mReaderAdapterX = "";
					for (UsbDevice device : mManager.getDeviceList().values()) {
						if (mReader.isSupported(device)) {
							mReaderAdapterX = device.getDeviceName().toString();
						}
					}

					UsbDevice device = (UsbDevice) intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (device != null && device.equals(mReader.getDevice())) {

						// Disable buttons
						//						mCloseButton.setEnabled(false);
						//						mPowerButton.setEnabled(false);       
						//						mControlButton.setEnabled(false);


						// Clear slot items


						// Close reader
						logMsg("Closing reader...");
						new CloseTask().execute();
					}
				}
			}
		}
	};

	private class OpenTask extends AsyncTask<UsbDevice, Void, Exception> {

		@Override
		protected Exception doInBackground(UsbDevice... params) {

			Exception result = null;

			try {

				mReader.open(params[0]);

			} catch (Exception e) {

				result = e;
			}

			return result;
		}

		@Override
		protected void onPostExecute(Exception result) {

			if (result != null) {

				logMsg(result.toString());

			} else {

				logMsg("Reader name: " + mReader.getReaderName());

				int numSlots = mReader.getNumSlots();
				logMsg("Number of slots: " + numSlots);

				// Add slot items


				// Remove all control codes
				//				mFeatures.clear();

				// Enable buttons
				TransmitParams params = new TransmitParams();
				params.slotNum = 0;
				params.controlCode = 3500;
				params.commandString="FF 00 52 00 00";		
				mOnSetDevice=true;
				new TransmitTask().execute(params);


				mCloseButton.setEnabled(true);
				//				mPowerButton.setEnabled(true);
				//				mControlButton.setEnabled(true);

			}
		}
	}

	private class CloseTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			mReader.close();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mOpenButton.setEnabled(true);
		}

	}



	private class TransmitParams {

		public int slotNum;
		public int controlCode;
		public String commandString;
	}

	private class TransmitProgress {

		public int controlCode=3500;
		public byte[] command;
		public int commandLength;
		public byte[] response;
		public int responseLength;
		public Exception e;
	}

	private class TransmitTask extends
	AsyncTask<TransmitParams, TransmitProgress, Void> {

		@Override
		protected Void doInBackground(TransmitParams... params) {

			TransmitProgress progress = null;

			byte[] command = null;
			byte[] response = null;
			int responseLength = 0;
			int foundIndex = 0;
			int startIndex = 0;

			do {

				// Find carriage return
				foundIndex = params[0].commandString.indexOf('\n', startIndex);
				if (foundIndex >= 0) {
					command = toByteArray(params[0].commandString.substring(
							startIndex, foundIndex));
				} else {
					command = toByteArray(params[0].commandString
							.substring(startIndex));
				}

				// Set next start index
				startIndex = foundIndex + 1;

				response = new byte[300];
				progress = new TransmitProgress();
				progress.controlCode = params[0].controlCode;
				try {

					if (params[0].controlCode < 0) {

						// Transmit APDU
						responseLength = mReader.transmit(params[0].slotNum,
								command, command.length, response,
								response.length);

					} else {

						// Transmit control command
						responseLength = mReader.control(params[0].slotNum,
								params[0].controlCode, command, command.length,
								response, response.length);
					}


					progress.command = command;
					progress.commandLength = command.length;					
					progress.response = response;
					progress.responseLength = responseLength;
					progress.e = null;

				} catch (Exception e) {

					progress.command = null;
					progress.commandLength = 0;
					progress.response = null;
					progress.responseLength = 0;
					progress.e = e;
				}

				publishProgress(progress);

			} while (foundIndex >= 0);

			return null;
		}

		@Override
		protected void onProgressUpdate(TransmitProgress... progress) {

			if (progress[0].e != null) {

				logMsg(progress[0].e.toString());

			} else {

				logMsg("Command:");
				logBuffer(progress[0].command, progress[0].commandLength);

				logMsg("Response:");
				logBuffer(progress[0].response, progress[0].responseLength);
				mCommand = logBufferResponseToText(progress[0].response, progress[0].responseLength);	
				mResponseHex = logBufferToText(progress[0].response, progress[0].responseLength);	

				//				if (progress[0].response != null && progress[0].responseLength > 0) {
				//
				//					int controlCode;
				//					int i;
				//
				//					// Show control codes for IOCTL_GET_FEATURE_REQUEST
				//					if (progress[0].controlCode == Reader.IOCTL_GET_FEATURE_REQUEST) {
				//
				//						mFeatures.fromByteArray(progress[0].response,progress[0].responseLength);
				//
				//						logMsg("Features:");
				//						for (i = Features.FEATURE_VERIFY_PIN_START; i <= Features.FEATURE_CCID_ESC_COMMAND; i++) {
				//
				//							controlCode = mFeatures.getControlCode(i);
				//							if (controlCode >= 0) {
				//								logMsg("Control Code: " + controlCode + " ("
				//										+ featureStrings[i] + ")");
				//							}
				//						}

				// Enable buttons if features are supported

				//					}

				//					controlCode = mFeatures.getControlCode(Features.FEATURE_IFD_PIN_PROPERTIES);
				//					if (controlCode >= 0
				//							&& progress[0].controlCode == controlCode) {
				//
				//						PinProperties pinProperties = new PinProperties(
				//								progress[0].response,
				//								progress[0].responseLength);
				//
				//						logMsg("PIN Properties:");
				//						logMsg("LCD Layout: "
				//								+ toHexString(pinProperties.getLcdLayout()));
				//						logMsg("Entry Validation Condition: "
				//								+ toHexString(pinProperties
				//										.getEntryValidationCondition()));
				//						logMsg("Timeout 2: "
				//								+ toHexString(pinProperties.getTimeOut2()));
				//					}

				//					controlCode = mFeatures.getControlCode(Features.FEATURE_GET_TLV_PROPERTIES);
				//					if (controlCode >= 0
				//							&& progress[0].controlCode == controlCode) {
				//
				//						TlvProperties readerProperties = new TlvProperties(
				//								progress[0].response,
				//								progress[0].responseLength);
				//
				//						Object property;
				//						logMsg("TLV Properties:");
				//						for (i = TlvProperties.PROPERTY_wLcdLayout; i <= TlvProperties.PROPERTY_wIdProduct; i++) {
				//
				//							property = readerProperties.getProperty(i);
				//							if (property instanceof Integer) {
				//								logMsg(propertyStrings[i] + ": "
				//										+ toHexString((Integer) property));
				//							} else if (property instanceof String) {
				//								logMsg(propertyStrings[i] + ": " + property);
				//							}
				//						}
				//					}
				//				}
			}
		}
		@Override
		protected void onPostExecute(Void result) {

			if(!mOnSetDevice){


				if(mTaskWriteStart==true){

					if(mCommand.trim().equals("90 00"))
					{
						final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
						tg.startTone(ToneGenerator.TONE_PROP_BEEP);
					}
					initStart = System.currentTimeMillis();
					timeEnd = initStart-nowWrite;	
					mWriteTime.setText(String.valueOf(timeEnd));

					timeEnd = initStart-nowStart;
					mEndTime.setText(String.valueOf(timeEnd));
					mTaskWriteStart=false;

				}

				if(mBlockCursur == 2 && mBlockRead){


					String strString = null;

					strString = hexToString(mResponseHex);

					//new startSoundTask().execute(strString);

					mTextType.setText(strString+" :");

				}
				if(mBlockCursur == 1 && mBlockRead){


					String strString = null;

					strString = hexToString(mResponseHex);

					mTextId.setText(strString+" :");

				}

				if(mBlockCursur == 0 && mBlockRead){

					String strString = null;

					strString = hexToString(mResponseHex);		

					try{
						mPoint = Float.parseFloat(strString);
						mTextBalance.setText(formatter.format(mPoint));	
					}catch (Exception e) {
						// TODO: handle exception
					}


				}

				checkStatus(mBlockCursur);

				if(mBlockCursur >= 0){



					if(!mAuthenticateRead ){

						if(mReadMode){

							mBlockCursur--;

							if(mBlockCursur == -1 && mAuthenticateRead==false && !mBlockRead){

								mTaskWriteStart=true;
								mTaskReadStart=false;

								if(mTaskReadStart==false){

									initStart = System.currentTimeMillis();
									timeEnd = initStart-nowRead;				
									mReadTime.setText(String.valueOf(timeEnd));
									mTaskReadStart=true;
									nowWrite = System.currentTimeMillis();
								}


								writer();

							}else{

								reader();

							}

						}

					}else if(mAuthenticateRead==true){
						if(mWriteMode){


							mBlockCursur--;
							try {
								writer();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

					}
				}
			}else if(mOnSetDevice){

				if(!mOnSetDevice2){
					int n =2;
					TransmitParams params = new TransmitParams();
					params.slotNum = 0;
					params.controlCode = 3500;
					params.commandString="FF 00 00 00 02 D4 04h";	
					if(n == 2){
						
					}
					mOnSetDevice2=true;
					new TransmitTask().execute(params);				
					
				}else if(mOnSetDevice2){					
					mOnSetDevice=false;
				}
			}
		}


	}

	private class startSoundTask extends AsyncTask<String, Void, Void>  {  

		protected Void doInBackground(String... params)   {

			String type = params[0].trim();
			final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

			if(type.equals("A")){

				tg.startTone(ToneGenerator.TONE_PROP_BEEP);

			}else if(type.equals("B")){

				tg.startTone(ToneGenerator.TONE_PROP_BEEP2);

			}
			else if(type.equals("C")){

				tg.startTone(ToneGenerator.TONE_DTMF_0);
			}
			else if(type.equals("D")){

				tg.startTone(ToneGenerator.TONE_DTMF_1);
			}
			else if(type.equals("E")){

				tg.startTone(ToneGenerator.TONE_DTMF_2);
			}

			return null;   	        
		}  

	}

	public void getUsbConnect(){

		mReaderAdapterX="";
		for (UsbDevice device : mManager.getDeviceList().values()) {
			if (mReader.isSupported(device)) {
				mReaderAdapterX = device.getDeviceName().toString();
			}
		}
	}

	public void getDevices(){

		boolean requested = false;

		// Disable open button
		mOpenButton.setEnabled(false);

		mReaderAdapterX="";

		for (UsbDevice device : mManager.getDeviceList().values()) {

			if (mReader.isSupported(device)) {
				mReaderAdapterX = device.getDeviceName();
			}
		}
		String deviceName = mReaderAdapterX;
		if (deviceName != null) {

			// For each device
			for (UsbDevice device : mManager.getDeviceList().values()) {

				// If device name is found
				if (deviceName.equals(device.getDeviceName())) {

					// Request permission
					mManager.requestPermission(device,
							mPermissionIntent);

					requested = true;		

					break;
				}
			}
		}

		if (!requested) {

			// Enable open button
			mOpenButton.setEnabled(true);
		}
	}


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setBlockNumber();
		// Get USB manager
		mManager = (UsbManager) getSystemService(Context.USB_SERVICE);

		// Initialize reader
		mReader = new Reader(mManager);
		mReader.setOnStateChangeListener(new OnStateChangeListener() {

			@Override
			public void onStateChange(int slotNum, int prevState, int currState) {

				if (prevState < Reader.CARD_UNKNOWN
						|| prevState > Reader.CARD_SPECIFIC) {
					prevState = Reader.CARD_UNKNOWN;
				}

				if (currState < Reader.CARD_UNKNOWN
						|| currState > Reader.CARD_SPECIFIC) {
					currState = Reader.CARD_UNKNOWN;
				}

				// Create output string
				final String outputString = "Slot " + slotNum + ": "
						+ stateStrings[prevState] + " -> "
						+ stateStrings[currState];
				final String state = stateStrings[currState];

				// Show output
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						logMsg(outputString);
						if(state.equals("Present"))
						{
							mAuthenticateRead=false;
							if(mReadMode)
							{	
								setBlockNumber();
								try {

									mTaskReadStart = false;
									mTaskStart = false;
									mTaskWriteStart = false;

									TimeRead = 0;
									TimeWrite = 0;
									timeEnd = 0;
									initStart = 0;
									nowRead = 0;
									nowWrite = 0;
									nowStart = 0;

									mBlockAuthen=true;
									mReadMode=true;
									mWriteMode=false;

									System.gc();

									reader();

								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}

						}
					}
				});
			}
		});

		// Register receiver for USB permission
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mReceiver, filter);

		sdf = new SimpleDateFormat("mm:ss.SSS");

		mResponseTextView = (TextView) findViewById(R.id.main_text_view_response);
		mResponseTextView.setMovementMethod(new ScrollingMovementMethod());
		mResponseTextView.setMaxLines(MAX_LINES);
		mResponseTextView.setText("");

		mTextType = (TextView)findViewById(R.id.textType);
		mReadTime = (TextView)findViewById(R.id.textViewReadSet);
		mWriteTime = (TextView)findViewById(R.id.textViewWriteSet);
		mEndTime = (TextView)findViewById(R.id.textViewEndSet);

		mWriteType = (EditText)findViewById(R.id.EditText_writeDataType);
		mQty = (EditText)findViewById(R.id.QTY);

		formatter = new DecimalFormat("###,###,###.##");

		mWriteDataBalanceEditText = (EditText)findViewById(R.id.EditText_writeDataBalance);
		mWriteDataIdEditText = (EditText)findViewById(R.id.EditText_writeDataId);

		mTextMode = (TextView)findViewById(R.id.textMode);
		mTextBalance = (TextView)findViewById(R.id.BALANCE);

		mTextId = (TextView)findViewById(R.id.textId);
		// Initialize reader spinner

		for (UsbDevice device : mManager.getDeviceList().values()) {
			if (mReader.isSupported(device)) {
				mReaderAdapterX = device.getDeviceName().toString();
			}
		}
		// Initialize open button
		mOpenButton = (Button) findViewById(R.id.btnStart);
		mOpenButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getDevices();
				// Disable open button
				mOpenButton.setEnabled(false);

			}
		});


		// Initialize close button
		mCloseButton = (Button) findViewById(R.id.btnStop);
		mCloseButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				// Disable buttons

				mOpenButton.setEnabled(false);
				// Clear slot items

				// Close reader
				logMsg("Closing reader...");
				new CloseTask().execute();
			}
		});

		mWriteData = (Button) findViewById(R.id.btnWrite);
		mWriteData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mAuthenticateRead==true)
				{
					mBlockWrite=true;
					setBlockNumber();
					mReadMode=false;
					mWriteMode=true;
					mTextMode.setText("- W R I T E -");
					//					mReadData.setVisibility(View.VISIBLE);
					//					mWriteData.setVisibility(View.GONE);
					try {
						writer();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else{
					Toast.makeText(getApplicationContext(), "Please Authenticate first", Toast.LENGTH_SHORT).show();
				}


			}
		});

		mReadData = (Button) findViewById(R.id.btnRead);
		mReadData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				mBlockAuthen=true;
				setBlockNumber();
				mReadMode=true;
				mWriteMode=false;
				mTextMode.setText("- R E A D -");
				//				mReadData.setVisibility(View.GONE);
				//				mWriteData.setVisibility(View.VISIBLE);

			}
		});


		// PIN verification command (ACOS3)
		byte[] pinVerifyData = { (byte) 0x80, 0x20, 0x06, 0x00, 0x08,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

		// Initialize PIN verify structure (ACOS3)
		mPinVerify.setTimeOut(0);
		mPinVerify.setTimeOut2(0);
		mPinVerify.setFormatString(0);
		mPinVerify.setPinBlockString(0x08);
		mPinVerify.setPinLengthFormat(0);
		mPinVerify.setPinMaxExtraDigit(0x0408);
		mPinVerify.setEntryValidationCondition(0x03);
		mPinVerify.setNumberMessage(0x01);
		mPinVerify.setLangId(0x0409);
		mPinVerify.setMsgIndex(0);
		mPinVerify.setTeoPrologue(0, 0);
		mPinVerify.setTeoPrologue(1, 0);
		mPinVerify.setTeoPrologue(2, 0);
		mPinVerify.setData(pinVerifyData, pinVerifyData.length);


		// PIN modification command (ACOS3)
		byte[] pinModifyData = { (byte) 0x80, 0x24, 0x00, 0x00, 0x08,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

		// Initialize PIN modify structure (ACOS3)
		mPinModify.setTimeOut(0);
		mPinModify.setTimeOut2(0);
		mPinModify.setFormatString(0);
		mPinModify.setPinBlockString(0x08);
		mPinModify.setPinLengthFormat(0);
		mPinModify.setInsertionOffsetOld(0);
		mPinModify.setInsertionOffsetNew(0);
		mPinModify.setPinMaxExtraDigit(0x0408);
		mPinModify.setConfirmPin(0x01);
		mPinModify.setEntryValidationCondition(0x03);
		mPinModify.setNumberMessage(0x02);
		mPinModify.setLangId(0x0409);
		mPinModify.setMsgIndex1(0);
		mPinModify.setMsgIndex2(0x01);
		mPinModify.setMsgIndex3(0);
		mPinModify.setTeoPrologue(0, 0);
		mPinModify.setTeoPrologue(1, 0);
		mPinModify.setTeoPrologue(2, 0);
		mPinModify.setData(pinModifyData, pinModifyData.length);

		// Initialize read key option
		mReadKeyOption.setTimeOut(0);
		mReadKeyOption.setPinMaxExtraDigit(0x0408);
		mReadKeyOption.setKeyReturnCondition(0x01);
		mReadKeyOption.setEchoLcdStartPosition(0);
		mReadKeyOption.setEchoLcdMode(0x01);




		// Disable buttons
		//		mCloseButton.setEnabled(false);
		//		mControlButton.setEnabled(false);


		// Hide input window
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	protected void onDestroy() {

		// Close reader
		mReader.close();

		// Unregister receiver
		unregisterReceiver(mReceiver);

		super.onDestroy();
	}
	protected void reader(){
		// Get slot number
		int slotNum = 0;

		mTaskReadStart=true;


		// If slot is selected

		if(!mTaskStart){

			mTaskStart=true;
			//			new chkProcessTimeTask().execute();
			nowStart = System.currentTimeMillis();
			nowRead = System.currentTimeMillis();

		}
		// Get control code
		int controlCode;
		try {

			controlCode = 3500;

		} catch (NumberFormatException e) {

			controlCode = Reader.IOCTL_CCID_ESCAPE;
		}

		// Set parameters
		TransmitParams params = new TransmitParams();
		params.slotNum = slotNum;
		params.controlCode = controlCode;


		if(mAuthenticateRead==false){


			params.commandString = getBlockLocation(mBlockCursur);

			//			if(mBlockCursur==10){
			//
			//				params.commandString = "FF 88 00 01 60 00";
			//
			//			}else if(mBlockCursur==9){
			//
			//				params.commandString = "FF 88 00 02 60 00";
			//
			//			}else if(mBlockCursur==8){
			//
			//				params.commandString = "FF 88 00 03 60 00";
			//
			//			}else if(mBlockCursur==7){
			//
			//				params.commandString = "FF 88 00 04 60 00";
			//
			//			}else if(mBlockCursur==6){
			//
			//				params.commandString = "FF 88 00 05 60 00";
			//
			//
			//			}else if(mBlockCursur==5){
			//
			//				params.commandString = "FF B0 00 01 10h";
			//
			//
			//			}else if(mBlockCursur==4){
			//
			//				params.commandString = "FF B0 00 02 10h";
			//
			//			}
			//			else if(mBlockCursur==3){
			//
			//				params.commandString = "FF B0 00 03 10h";
			//
			//
			//			}else if(mBlockCursur==2){
			//
			//				params.commandString = "FF B0 00 04 10h";
			//
			//			}
			//			else if(mBlockCursur==1){
			//
			//				params.commandString = "FF B0 00 05 10h";
			//
			//			}

		}


		// Transmit control command
		if(params.commandString!=null && !params.commandString.equals("")){

			logMsg("Slot " + slotNum
					+ ": Transmitting control command (Control Code: "
					+ params.controlCode + ")...");

			new TransmitTask().execute(params);

		}else{
			if(mBlockAuthen){
				logMsg("End Authen...");
			}
			if(mBlockRead){
				logMsg("End Read...");
			}
			if(mBlockWrite){
				logMsg("End Write...");
			}
		}
	}

	protected void writer(){
		// Get slot number
		int slotNum = 0;

		// If slot is selected
		if (slotNum != Spinner.INVALID_POSITION) {

			// Get control code
			int controlCode;
			try {

				controlCode = 3500;

			} catch (NumberFormatException e) {

				controlCode = Reader.IOCTL_CCID_ESCAPE;
			}

			// Set parameters
			TransmitParams params = new TransmitParams();
			params.slotNum = slotNum;
			params.controlCode = controlCode;
			params.commandString="";

			if(mBlockCursur == -1 && mAuthenticateRead==false){

				mAuthenticateRead=true;

				String strQty = mQty.getText().toString();
				String strPayment = mTextBalance.getText().toString();

				if(!strQty.trim().isEmpty() && mPoint!=0 ){

					float payment = Float.parseFloat(splitComma(strPayment));
					float qty = Float.parseFloat(strQty);

					if(payment > qty){

						float result  = payment - qty ;

						mTextBalance.setText(formatter.format(result));
						String stringToConvert = String.valueOf(result);

						String strByte = toHexString(stringToConvert.getBytes());					
						strByte = chkByteSize(strByte);

						params.commandString = "FF D6 00 04 10 "+strByte;

						mTaskWriteStart = true;

					}
				}

			}else if(mBlockCursur!= -1){
				String stringToConvert = null;

				if(mBlockCursur == 2 ){

					stringToConvert = mWriteType.getText().toString();

				}

				else if(mBlockCursur == 1 ){

					stringToConvert = mWriteDataIdEditText.getText().toString();

				}

				else if(mBlockCursur == 0 ){

					stringToConvert = mWriteDataBalanceEditText.getText().toString();

				}

				if(!stringToConvert.equals("") && stringToConvert != null)
				{	

					String strByte = toHexString(stringToConvert.getBytes());
					strByte = chkByteSize(strByte);
					params.commandString = getBlockLocation(mBlockCursur)+strByte;

				}else{

					mBlockCursur--;
					writer();

				}

			}



			// Transmit control command


			if(params.commandString!=null && !params.commandString.equals("")){

				logMsg("Slot " + slotNum
						+ ": Transmitting control command (Control Code: "
						+ params.controlCode + ")...");

				new TransmitTask().execute(params);

			}else if(mBlockCursur == -1){
				mTaskStart=false;

				if(mTaskStart=false){
					initStart = System.currentTimeMillis();
					timeEnd = nowStart-initStart;				
					mEndTime.setText(String.valueOf(timeEnd));

					timeEnd = nowWrite-initStart;				
					mWriteTime.setText(String.valueOf(timeEnd));

					mTaskStart=true;

				}
				logMsg("End Read...");
			}else{

				mTaskStart=false;

				if(mTaskStart=false){
					initStart = System.currentTimeMillis();
					timeEnd = nowStart-initStart;				
					mEndTime.setText(String.valueOf(timeEnd));

					timeEnd = nowWrite-initStart;				
					mWriteTime.setText(String.valueOf(timeEnd));

					mTaskStart=true;

				}
				logMsg("End Write...");
			}

		}
	}

	/**
	 * Logs the message.
	 * 
	 * @param msg
	 *            the message.
	 */
	private void logMsg(String msg) {

		DateFormat dateFormat = new SimpleDateFormat("[dd-MM-yyyy HH:mm:ss]: ");
		Date date = new Date();
		String oldMsg = mResponseTextView.getText().toString();

		mResponseTextView.setText(oldMsg + "\n" + dateFormat.format(date) + msg);

		if (mResponseTextView.getLineCount() > MAX_LINES) {
			mResponseTextView.scrollTo(0,
					(mResponseTextView.getLineCount() - MAX_LINES)
					* mResponseTextView.getLineHeight());
		}
	}

	/**
	 * Logs the contents of buffer.
	 * 
	 * @param buffer
	 *            the buffer.
	 * @param bufferLength
	 *            the buffer length.
	 */
	private void logBuffer(byte[] buffer, int bufferLength) {

		String bufferString = "";

		for (int i = 0; i < bufferLength; i++) {

			String hexChar = Integer.toHexString(buffer[i] & 0xFF);
			if (hexChar.length() == 1) {
				hexChar = "0" + hexChar;
			}

			if (i % 16 == 0) {

				if (bufferString != "") {

					logMsg(bufferString);
					bufferString = "";
				}
			}

			bufferString += hexChar.toUpperCase() + " ";
		}

		if (bufferString != "") {
			logMsg(bufferString);
		}
	}

	private String logBufferToText(byte[] buffer, int bufferLength) {

		String bufferString = "";

		for (int i = 0; i < bufferLength; i++) {

			String hexChar = Integer.toHexString(buffer[i] & 0xFF);
			if (hexChar.length() == 1) {
				return bufferString;
				//hexChar = "0" + hexChar;
			}

			if (i % 16 == 0) {

				if (bufferString != "") {				
					bufferString = "";
				}
			}

			bufferString += hexChar.toUpperCase() + " ";
		}

		if (bufferString != "") {
		}

		return bufferString;
	}

	private String logBufferResponseToText(byte[] buffer, int bufferLength) {

		String bufferString = "";

		for (int i = 0; i < bufferLength; i++) {

			String hexChar = Integer.toHexString(buffer[i] & 0xFF);
			if (hexChar.length() == 1) {
				//return bufferString;
				hexChar = "0" + hexChar;
			}

			if (i % 16 == 0) {

				if (bufferString != "") {				
					bufferString = "";
				}
			}

			bufferString += hexChar.toUpperCase() + " ";
		}

		if (bufferString != "") {
		}

		return bufferString;
	}

	/**
	 * Converts the HEX string to byte array.
	 * 
	 * @param hexString
	 *            the HEX string.
	 * @return the byte array.
	 */
	private byte[] toByteArray(String hexString) {

		int hexStringLength = hexString.length();
		byte[] byteArray = null;
		int count = 0;
		char c;
		int i;

		// Count number of hex characters
		for (i = 0; i < hexStringLength; i++) {

			c = hexString.charAt(i);
			if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
					&& c <= 'f') {
				count++;
			}
		}

		byteArray = new byte[(count + 1) / 2];
		boolean first = true;
		int len = 0;
		int value;
		for (i = 0; i < hexStringLength; i++) {

			c = hexString.charAt(i);
			if (c >= '0' && c <= '9') {
				value = c - '0';
			} else if (c >= 'A' && c <= 'F') {
				value = c - 'A' + 10;
			} else if (c >= 'a' && c <= 'f') {
				value = c - 'a' + 10;
			} else {
				value = -1;
			}

			if (value >= 0) {

				if (first) {

					byteArray[len] = (byte) (value << 4);

				} else {

					byteArray[len] |= value;
					len++;
				}

				first = !first;
			}
		}

		return byteArray;
	}

	/**
	 * Converts the integer to HEX string.
	 * 
	 * @param i
	 *            the integer.
	 * @return the HEX string.
	 */
	private String toHexString(int i) {

		String hexString = Integer.toHexString(i);
		if (hexString.length() % 2 != 0) {
			hexString = "0" + hexString;
		}

		return hexString.toUpperCase();
	}

	/**
	 * Converts the byte array to HEX string.
	 * 
	 * @param buffer
	 *            the buffer.
	 * @return the HEX string.
	 */
	private String toHexString(byte[] buffer) {

		String bufferString = "";

		for (int i = 0; i < buffer.length; i++) {

			String hexChar = Integer.toHexString(buffer[i] & 0xFF);
			if (hexChar.length() == 1) {
				hexChar = "0" + hexChar;
			}

			bufferString += hexChar.toUpperCase() + " ";
		}

		return bufferString;
	}

	/**
	 * Converts the hex.
	 * 
	 * @param buffer
	 *            the buffer.
	 * @return the HEX string.
	 */
	public  String hexToString(String hex) {

		StringBuilder sb = new StringBuilder();
		String[] splitSpace = hex.split("\\s+");

		hex="";

		for(int i=0 ; i < splitSpace.length ; i++){

			hex=hex+splitSpace[i];

		}


		char[] hexData = hex.toCharArray();

		for (int count = 0; count < hexData.length - 1; count += 2) {

			int firstDigit = Character.digit(hexData[count], 16);

			int lastDigit = Character.digit(hexData[count + 1], 16);

			int decimal = firstDigit * 16 + lastDigit;

			sb.append((char)decimal);
		}
		return sb.toString();
	}

	public  String chkByteSize(String strByte) {

		String[] chkSize = strByte.split("\\s+");

		int i=chkSize.length;

		while(i < 16){

			strByte = strByte+ " 00";
			i++;
		}
		return strByte;
	}

	private String splitComma(String commaNumber){

		String[] comma = commaNumber.split(",");
		String numberReturn = "";

		for(int i=0 ; i < comma.length;i++){
			numberReturn = numberReturn+comma[i];
		}

		return numberReturn;

	}
	private byte[] Encrypter(byte[] input) throws Exception
	{

		// Security.addProvider(new MainActivity().BouncyCastleProvider());    

		byte[] keyBytes = new byte[] {  0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 ,0x08 ,0x09 ,0x0a ,0x0b ,0x0c ,0x0d ,0x0e ,0x0f ,0x10 };

		SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");

		//mTxtInput.setText(new String(data));
		//System.out.println(new String(input));

		// encryption pass
		cipher.init(Cipher.ENCRYPT_MODE, key);

		byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
		int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
		ctLength += cipher.doFinal(cipherText, ctLength);

		return cipherText;
		//System.out.println(new String(cipherText));
		//System.out.println(ctLength);

		// mTxtCipher.setText(new String(cipherText));
		// mTxtCtLenght.setText(String.valueOf(ctLength));
		// decryption pass

	}
	//	private byte[] Decryptor(byte[] cipherText) throws Exception
	//	{
	//
	//		byte[] keyBytes = new byte[] {  0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 ,0x08 ,0x09 ,0x0a ,0x0b ,0x0c ,0x0d ,0x0e ,0x0f ,0x10 };
	//
	//		SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
	//
	//		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
	//
	//		int x = cipherText.length;
	//
	//
	//		// decryption pass
	//		cipher.init(Cipher.DECRYPT_MODE, key);
	//		byte[] plainText = new byte[cipher.getOutputSize(x)];
	//		int ptLength = cipher.update(cipherText, 0, x, plainText, 0);
	//
	//		ptLength += cipher.doFinal(plainText, ptLength);  	    
	//
	//		return plainText;
	//		//mTxtPlainText.setText(new String(plainText));
	//		//mTxtPtLength.setText(String.valueOf(ptLength));
	//	}
	//	private String getEncrypt(String covertValue) throws Exception{
	//
	//		byte[] theByteArray = covertValue.getBytes();
	//
	//		String strByte = toHexString(theByteArray);
	//
	//		//strByte = chkByteSize(strByte);
	//
	//		theByteArray = strByte.getBytes();
	//
	//		theByteArray = Encrypter(theByteArray);
	//
	//		strByte = toHexString(theByteArray);
	//
	//		return strByte;
	//
	//	}
	//	private String getDecrypt(String HexString)throws Exception{
	//
	//		byte[] theByteArray = HexString.getBytes();
	//
	//		theByteArray = Decryptor(theByteArray);
	//
	//		HexString = toHexString(theByteArray);
	//
	//		HexString = hexToString(HexString);
	//
	//
	//
	//		return HexString;
	//
	//	}
	private String getBlockLocation(int count){
		String location = "";
		if(count < 0 ){
			setBlockNumber();
			count = mBlockCursur;
		}

		String[] locationSet = {"04","05","06"};

		//String[] authenLocation  = {"00","01","02","03"};

		//"0E","0D","0C","0B","0A","09","08","07","06","05",

		if(mBlockAuthen){

			if(count <= 3){
				location = "00";			
			}else if(count > 3 && count <= 7)
			{
				location = "01";
			}else if(count > 7 && count <= 11)
			{
				location = "02";
			}else if(count > 11 && count <= 15)
			{
				location = "03";
			}

			location = "FF 88 00 04 60 00";

			mBlockCursur = 0;

		}else if(mBlockRead){

			location = "FF B0 00 "+locationSet[count]+" 10"	;

		}else if(mBlockWrite){

			location = "FF D6 00 "+locationSet[count]+" 10 ";

		}

		return location;

	}

	public void checkStatus(int count){

		if(mBlockAuthen){

			if(count == 0){
				mBlockAuthen=false;
				mBlockRead=true;
			}

		}else if(mBlockRead){
			if(count == 0){
				mBlockRead=false;
				mBlockWrite=true;
			}

		}else if(mBlockWrite){

			if(count == 0){
				mBlockWrite=false;
			}
		}
	}

	private void setBlockNumber(){

		mBlockCursur = 2;

	}

	//	private class chkProcessTimeTask extends AsyncTask<Void, Void, Void>  {  
	//		
	//		long timeEnd,TimeRead,TimeWrite;
	//		
	//		protected Void doInBackground(Void... params)   {
	//			
	//			long initRead = 0,initWrite = 0,initStart = 0,
	//					nowRead,nowWrite,nowStart;
	//
	//			while(mTaskStart){
	//
	//
	//				nowStart = System.currentTimeMillis();
	//				timeEnd = nowStart-initStart;
	//
	//
	//				if(mTaskReadStart){
	//
	//					nowRead=System.currentTimeMillis();
	//					TimeRead = nowRead - initRead;
	//
	//
	//				}
	//				if(mTaskWriteStart){
	//
	//					nowWrite= System.currentTimeMillis();
	//					TimeWrite = nowWrite - initWrite;
	//
	//
	//				}
	//
	//			}
	//
	//			return null;   	        
	//		} 
	//		protected void onPostExecute(Void result) {
	//
	//			mEndTime.setText(String.valueOf(timeEnd));
	//			mReadTime.setText(String.valueOf(TimeRead));
	//			mWriteTime.setText(String.valueOf(TimeWrite));
	//
	//		}		
	//
	//	}


}
