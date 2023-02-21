/*
 * Copyright (C) 2011-2013 Advanced Card Systems Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Advanced
 * Card Systems Ltd. ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with ACS.
 */

package com.cczk.lxp.disinfectsystem.utils.hw;

import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.acs.smartcard.Features;
import com.acs.smartcard.PinProperties;
import com.acs.smartcard.Reader;
import com.acs.smartcard.Reader.OnStateChangeListener;
import com.acs.smartcard.TlvProperties;
import com.cczk.lxp.disinfectsystem.test.NFCTestActivity;
import com.cczk.lxp.disinfectsystem.view.activity.MainActivity;

/**
 * Test program for ACS smart card readers.
 *
 * @author Godfrey Chung
 * @version 1.1.1, 16 Apr 2013
 */
public class NFCUtils {
    public static final String TAG  = "NFCUtils";
    public static boolean IsHaveDevice=false;
    public static boolean IsHaveDeviceName=false;
    private Context MyContext;

    private  UsbDevice device;
    private Handler FrgmHandler;

    //获取单例
    private static NFCUtils instance=null;
    public static NFCUtils getInstance() {
        if (instance == null) {
            synchronized (NFCUtils.class) {
                if (instance == null) {
                    instance = new NFCUtils();
                }
            }
        }
        return instance;
    }


    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static final String[] powerActionStrings = { "Power Down",
            "Cold Reset", "Warm Reset" };

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

    private static final int MAX_LINES = 25;
    private ArrayAdapter<String> mReaderAdapter;
    private ArrayAdapter<String> mSlotAdapter;

    private float ReadIdDelay =0;
    private int LastCmdState =0;    // 0 无特殊 1Cmd命令 2Rsp回应
    private boolean LastCmdSuccess=false;

    private int LastCmdType =Type_Null;
    private static final int Type_Null = 0;
    private static final int Type_ID = 1;
    private static final int Type_SetData = 2;
    private static final int Type_GetData = 3;

    public ArrayList<String> ResultList=new ArrayList<>();

    private Features mFeatures = new Features();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {

                synchronized (this) {

                     device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                        if (device != null) {

                            // Open reader
                            logMsg("Opening reader: " + device.getDeviceName()
                                    + "...");
                            new OpenTask().execute(device);

                            IsHaveDevice=true;
                        }else{
                            IsHaveDevice=false;
                        }

                    } else {

                        logMsg("Permission denied for device "
                                + device.getDeviceName());

                        // Enable open button
                        //mOpenButton.setEnabled(true);
                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                synchronized (this) {

                    // Update reader list
                    mReaderAdapter.clear();
                    for (UsbDevice device : mManager.getDeviceList().values()) {
                        if (mReader.isSupported(device)) {
                            mReaderAdapter.add(device.getDeviceName());
                            Log.v(TAG,device.getDeviceName() );
                        }
                    }

                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (device != null && device.equals(mReader.getDevice())) {

                        // Disable buttons
                        //LXP 隐藏按钮

                        // Clear slot items
                        mSlotAdapter.clear();

                        // Close reader
                        logMsg("Closing reader...");
                        new CloseTask().execute();

                        IsHaveDevice=false;
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
                mSlotAdapter.clear();
                for (int i = 0; i < numSlots; i++) {
                    mSlotAdapter.add(Integer.toString(i));
                }

                // Remove all control codes
                mFeatures.clear();

                // Enable buttons
                //LXP 显示按钮
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
            //mOpenButton.setEnabled(true);
        }

    }

    private class PowerParams {

        public int slotNum;
        public int action;
    }

    private class PowerResult {

        public byte[] atr;
        public Exception e;
    }

    private class PowerTask extends AsyncTask<PowerParams, Void, PowerResult> {

        @Override
        protected PowerResult doInBackground(PowerParams... params) {

            PowerResult result = new PowerResult();

            try {

                result.atr = mReader.power(params[0].slotNum, params[0].action);

            } catch (Exception e) {

                result.e = e;
            }

            return result;
        }

        @Override
        protected void onPostExecute(PowerResult result) {

            if (result.e != null) {

                logMsg(result.e.toString());

            } else {

                // Show ATR
                if (result.atr != null) {

                    logMsg("ATR:");
                    logBuffer(result.atr, result.atr.length);

                } else {

                    logMsg("ATR: None");
                }
            }
        }
    }

    private class SetProtocolParams {

        public int slotNum;
        public int preferredProtocols;
    }

    private class SetProtocolResult {

        public int activeProtocol;
        public Exception e;
    }

    private class SetProtocolTask extends
            AsyncTask<SetProtocolParams, Void, SetProtocolResult> {

        @Override
        protected SetProtocolResult doInBackground(SetProtocolParams... params) {

            SetProtocolResult result = new SetProtocolResult();

            try {

                result.activeProtocol = mReader.setProtocol(params[0].slotNum,
                        params[0].preferredProtocols);

            } catch (Exception e) {

                result.e = e;
            }

            return result;
        }

        @Override
        protected void onPostExecute(SetProtocolResult result) {

            if (result.e != null) {

                logMsg(result.e.toString());

            } else {

                String activeProtocolString = "Active Protocol: ";

                switch (result.activeProtocol) {

                    case Reader.PROTOCOL_T0:
                        activeProtocolString += "T=0";
                        break;

                    case Reader.PROTOCOL_T1:
                        activeProtocolString += "T=1";
                        break;

                    default:
                        activeProtocolString += "Unknown";
                        break;
                }

                // Show active protocol
                logMsg(activeProtocolString);
            }
        }
    }

    private class TransmitParams {

        public int slotNum;
        public int controlCode;
        public String commandString;
    }

    private class TransmitProgress {

        public int controlCode;
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

                response = new byte[65538];
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

        //处理信息
        @Override
        protected void onProgressUpdate(TransmitProgress... progress) {

            if (progress[0].e != null) {

                logMsg(progress[0].e.toString());

            } else {
                String data="";

                logMsg("Command:");
                logBuffer(progress[0].command, progress[0].commandLength);

                logMsg("Response:");
                logBuffer(progress[0].response, progress[0].responseLength);

                if (progress[0].response != null
                        && progress[0].responseLength > 0) {

                    int controlCode;
                    int i;

                    // Show control codes for IOCTL_GET_FEATURE_REQUEST
                    if (progress[0].controlCode == Reader.IOCTL_GET_FEATURE_REQUEST) {

                        mFeatures.fromByteArray(progress[0].response,
                                progress[0].responseLength);

                        logMsg("Features:");
                        for (i = Features.FEATURE_VERIFY_PIN_START; i <= Features.FEATURE_CCID_ESC_COMMAND; i++) {

                            controlCode = mFeatures.getControlCode(i);
                            if (controlCode >= 0) {
                                logMsg("Control Code: " + controlCode + " ("
                                        + featureStrings[i] + ")");
                            }
                        }

                        // Enable buttons if features are supported
//                        mVerifyPinButton
//                                .setEnabled(mFeatures
//                                        .getControlCode(Features.FEATURE_VERIFY_PIN_DIRECT) >= 0);
//                        mModifyPinButton
//                                .setEnabled(mFeatures
//                                        .getControlCode(Features.FEATURE_MODIFY_PIN_DIRECT) >= 0);
                    }

                    controlCode = mFeatures
                            .getControlCode(Features.FEATURE_IFD_PIN_PROPERTIES);
                    if (controlCode >= 0
                            && progress[0].controlCode == controlCode) {

                        PinProperties pinProperties = new PinProperties(
                                progress[0].response,
                                progress[0].responseLength);

                        logMsg("PIN Properties:");
                        logMsg("LCD Layout: "
                                + toHexString(pinProperties.getLcdLayout()));
                        logMsg("Entry Validation Condition: "
                                + toHexString(pinProperties
                                .getEntryValidationCondition()));
                        logMsg("Timeout 2: "
                                + toHexString(pinProperties.getTimeOut2()));
                    }

                    controlCode = mFeatures
                            .getControlCode(Features.FEATURE_GET_TLV_PROPERTIES);
                    if (controlCode >= 0
                            && progress[0].controlCode == controlCode) {

                        TlvProperties readerProperties = new TlvProperties(
                                progress[0].response,
                                progress[0].responseLength);

                        Object property;
                        logMsg("TLV Properties:");
                        for (i = TlvProperties.PROPERTY_wLcdLayout; i <= TlvProperties.PROPERTY_wIdProduct; i++) {

                            property = readerProperties.getProperty(i);
                            if (property instanceof Integer) {
                                logMsg(propertyStrings[i] + ": "
                                        + toHexString((Integer) property));
                            } else if (property instanceof String) {
                                logMsg(propertyStrings[i] + ": " + property);
                            }
                        }
                    }
                }
            }
        }
    }


    public void Init(final Activity activity)
    {
        MyContext=activity;
        mManager = (UsbManager)MyContext.getSystemService(Context.USB_SERVICE);

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

                // Show output
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logMsg(outputString);
                    }
                });

            }
        });

        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(MyContext, 0, new Intent(
                ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        //0422
        //MyContext.registerReceiver(mReceiver, filter);

        if(MyContext!=null)
        {
            try{
                MyContext.registerReceiver(mReceiver, filter);
            }catch (Exception e){}
        }

        // Initialize response text view
//        mResponseTextView = (TextView) findViewById(R.id.main_text_view_response);
//        mResponseTextView.setMovementMethod(new ScrollingMovementMethod());
//        mResponseTextView.setMaxLines(MAX_LINES);
//        mResponseTextView.setText("");

        // Initialize reader spinner
        mReaderAdapter = new ArrayAdapter<String>(MyContext,
                android.R.layout.simple_spinner_item);
        for (UsbDevice device : mManager.getDeviceList().values()) {
            if (mReader.isSupported(device)) {
                mReaderAdapter.add(device.getDeviceName());
            }
        }
//        mReaderSpinner = (Spinner) findViewById(R.id.main_spinner_reader);
//        mReaderSpinner.setAdapter(mReaderAdapter);

        // Initialize slot spinner
        mSlotAdapter = new ArrayAdapter<String>(MyContext,
                android.R.layout.simple_spinner_item);
//        mSlotSpinner = (Spinner) findViewById(R.id.main_spinner_slot);
//        mSlotSpinner.setAdapter(mSlotAdapter);

        // Initialize power spinner
        ArrayAdapter<String> powerAdapter = new ArrayAdapter<String>(MyContext,
                android.R.layout.simple_spinner_item, powerActionStrings);
//        mPowerSpinner = (Spinner) findViewById(R.id.main_spinner_power);
//        mPowerSpinner.setAdapter(powerAdapter);
//        mPowerSpinner.setSelection(Reader.CARD_WARM_RESET);

        // Initialize list button
//        mListButton = (Button) findViewById(R.id.main_button_list);
//        mListButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                mReaderAdapter.clear();
//                for (UsbDevice device : mManager.getDeviceList().values()) {
//                    if (mReader.isSupported(device)) {
//                        mReaderAdapter.add(device.getDeviceName());
//                    }
//                }
//            }
//        });
//
//        // Initialize open button
//        mOpenButton = (Button) findViewById(R.id.main_button_open);
//        mOpenButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                boolean requested = false;
//
//                // Disable open button
//                mOpenButton.setEnabled(false);
//
//                String deviceName = (String) mReaderSpinner.getSelectedItem();
//
//                if (deviceName != null) {
//
//                    // For each device
//                    for (UsbDevice device : mManager.getDeviceList().values()) {
//
//                        // If device name is found
//                        if (deviceName.equals(device.getDeviceName())) {
//
//                            // Request permission
//                            mManager.requestPermission(device,
//                                    mPermissionIntent);
//
//                            requested = true;
//                            break;
//                        }
//                    }
//                }
//
//                if (!requested) {
//
//                    // Enable open button
//                    mOpenButton.setEnabled(true);
//                }
//            }
//        });
//
//        // Initialize close button
//        mCloseButton = (Button) findViewById(R.id.main_button_close);
//        mCloseButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                // Disable buttons
//                mCloseButton.setEnabled(false);
//                mSlotSpinner.setEnabled(false);
//                mGetStateButton.setEnabled(false);
//                mPowerSpinner.setEnabled(false);
//                mPowerButton.setEnabled(false);
//                mGetAtrButton.setEnabled(false);
//                mT0CheckBox.setEnabled(false);
//                mT1CheckBox.setEnabled(false);
//                mSetProtocolButton.setEnabled(false);
//                mGetProtocolButton.setEnabled(false);
//                mTransmitButton.setEnabled(false);
//                mControlButton.setEnabled(false);
//                mGetFeaturesButton.setEnabled(false);
//                mVerifyPinButton.setEnabled(false);
//                mModifyPinButton.setEnabled(false);
//                mReadKeyButton.setEnabled(false);
//                mDisplayLcdMessageButton.setEnabled(false);
//
//                // Clear slot items
//                mSlotAdapter.clear();
//
//                // Close reader
//                logMsg("Closing reader...");
//                new CloseTask().execute();
//            }
//        });
//
//        // Initialize get state button
//        mGetStateButton = (Button) findViewById(R.id.main_button_get_state);
//        mGetStateButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                // Get slot number
//                int slotNum = mSlotSpinner.getSelectedItemPosition();
//
//                // If slot is selected
//                if (slotNum != Spinner.INVALID_POSITION) {
//
//                    try {
//
//                        // Get state
//                        logMsg("Slot " + slotNum + ": Getting state...");
//                        int state = mReader.getState(slotNum);
//
//                        if (state < Reader.CARD_UNKNOWN
//                                || state > Reader.CARD_SPECIFIC) {
//                            state = Reader.CARD_UNKNOWN;
//                        }
//
//                        logMsg("State: " + stateStrings[state]);
//
//                    } catch (IllegalArgumentException e) {
//
//                        logMsg(e.toString());
//                    }
//                }
//            }
//        });
//
//        // Initialize power button
//        mPowerButton = (Button) findViewById(R.id.main_button_power);
//        mPowerButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                // Get slot number
//                int slotNum = mSlotSpinner.getSelectedItemPosition();
//
//                // Get action number
//                int actionNum = mPowerSpinner.getSelectedItemPosition();
//
//                // If slot and action are selected
//                if (slotNum != Spinner.INVALID_POSITION
//                        && actionNum != Spinner.INVALID_POSITION) {
//
//                    if (actionNum < Reader.CARD_POWER_DOWN
//                            || actionNum > Reader.CARD_WARM_RESET) {
//                        actionNum = Reader.CARD_WARM_RESET;
//                    }
//
//                    // Set parameters
//                    PowerParams params = new PowerParams();
//                    params.slotNum = slotNum;
//                    params.action = actionNum;
//
//                    // Perform power action
//                    logMsg("Slot " + slotNum + ": "
//                            + powerActionStrings[actionNum] + "...");
//                    new PowerTask().execute(params);
//                }
//            }
//        });
//
//        mGetAtrButton = (Button) findViewById(R.id.main_button_get_atr);
//        mGetAtrButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                // Get slot number
//                int slotNum = mSlotSpinner.getSelectedItemPosition();
//
//                // If slot is selected
//                if (slotNum != Spinner.INVALID_POSITION) {
//
//                    try {
//
//                        // Get ATR
//                        logMsg("Slot " + slotNum + ": Getting ATR...");
//                        byte[] atr = mReader.getAtr(slotNum);
//
//                        // Show ATR
//                        if (atr != null) {
//
//                            logMsg("ATR:");
//                            logBuffer(atr, atr.length);
//
//                        } else {
//
//                            logMsg("ATR: None");
//                        }
//
//                    } catch (IllegalArgumentException e) {
//
//                        logMsg(e.toString());
//                    }
//                }
//            }
//        });
//
//        // Initialize T=0 check box
//        mT0CheckBox = (CheckBox) findViewById(R.id.main_check_box_t0);
//        mT0CheckBox.setChecked(true);
//
//        // Initialize T=1 check box
//        mT1CheckBox = (CheckBox) findViewById(R.id.main_check_box_t1);
//        mT1CheckBox.setChecked(true);
//
//        // Initialize set protocol button
//        mSetProtocolButton = (Button) findViewById(R.id.main_button_set_protocol);
//        mSetProtocolButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                // Get slot number
//                int slotNum = mSlotSpinner.getSelectedItemPosition();
//
//                // If slot is selected
//                if (slotNum != Spinner.INVALID_POSITION) {
//
//                    int preferredProtocols = Reader.PROTOCOL_UNDEFINED;
//                    String preferredProtocolsString = "";
//
//                    if (mT0CheckBox.isChecked()) {
//
//                        preferredProtocols |= Reader.PROTOCOL_T0;
//                        preferredProtocolsString = "T=0";
//                    }
//
//                    if (mT1CheckBox.isChecked()) {
//
//                        preferredProtocols |= Reader.PROTOCOL_T1;
//                        if (preferredProtocolsString != "") {
//                            preferredProtocolsString += "/";
//                        }
//
//                        preferredProtocolsString += "T=1";
//                    }
//
//                    if (preferredProtocolsString == "") {
//                        preferredProtocolsString = "None";
//                    }
//
//                    // Set Parameters
//                    SetProtocolParams params = new SetProtocolParams();
//                    params.slotNum = slotNum;
//                    params.preferredProtocols = preferredProtocols;
//
//                    // Set protocol
//                    logMsg("Slot " + slotNum + ": Setting protocol to "
//                            + preferredProtocolsString + "...");
//                    new SetProtocolTask().execute(params);
//                }
//            }
//        });
//
//        // Initialize get active protocol button
//        mGetProtocolButton = (Button) findViewById(R.id.main_button_get_protocol);
//        mGetProtocolButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                // Get slot number
//                int slotNum = mSlotSpinner.getSelectedItemPosition();
//
//                // If slot is selected
//                if (slotNum != Spinner.INVALID_POSITION) {
//
//                    try {
//
//                        // Get active protocol
//                        logMsg("Slot " + slotNum
//                                + ": Getting active protocol...");
//                        int activeProtocol = mReader.getProtocol(slotNum);
//
//                        // Show active protocol
//                        String activeProtocolString = "Active Protocol: ";
//                        switch (activeProtocol) {
//
//                            case Reader.PROTOCOL_T0:
//                                activeProtocolString += "T=0";
//                                break;
//
//                            case Reader.PROTOCOL_T1:
//                                activeProtocolString += "T=1";
//                                break;
//
//                            default:
//                                activeProtocolString += "Unknown";
//                                break;
//                        }
//
//                        logMsg(activeProtocolString);
//
//                    } catch (IllegalArgumentException e) {
//
//                        logMsg(e.toString());
//                    }
//                }
//            }
//        });
//
//        // Initialize command edit text
//        mCommandEditText = (EditText) findViewById(R.id.main_edit_text_command);
//
//        // Initialize transmit button
//        mTransmitButton = (Button) findViewById(R.id.main_button_transmit);
//        mTransmitButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                // Get slot number
//                int slotNum = mSlotSpinner.getSelectedItemPosition();
//
//                // If slot is selected
//                if (slotNum != Spinner.INVALID_POSITION) {
//
//                    // Set parameters
//                    TransmitParams params = new TransmitParams();
//                    params.slotNum = slotNum;
//                    params.controlCode = -1;
//                    params.commandString = mCommandEditText.getText()
//                            .toString();
//
//                    // Transmit APDU
//                    logMsg("Slot " + slotNum + ": Transmitting APDU...");
//                    new TransmitTask().execute(params);
//                }
//            }
//        });
//
//        // Initialize control edit text
//        mControlEditText = (EditText) findViewById(R.id.main_edit_text_control);
//        mControlEditText.setText(Integer.toString(Reader.IOCTL_CCID_ESCAPE));
//
//        // Initialize control button
//        mControlButton = (Button) findViewById(R.id.main_button_control);
//        mControlButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                // Get slot number
//                int slotNum = mSlotSpinner.getSelectedItemPosition();
//
//                // If slot is selected
//                if (slotNum != Spinner.INVALID_POSITION) {
//
//                    // Get control code
//                    int controlCode;
//                    try {
//
//                        controlCode = Integer.parseInt(mControlEditText
//                                .getText().toString());
//
//                    } catch (NumberFormatException e) {
//
//                        controlCode = Reader.IOCTL_CCID_ESCAPE;
//                    }
//
//                    // Set parameters
//                    TransmitParams params = new TransmitParams();
//                    params.slotNum = slotNum;
//                    params.controlCode = controlCode;
//                    params.commandString = mCommandEditText.getText()
//                            .toString();
//
//                    // Transmit control command
//                    logMsg("Slot " + slotNum
//                            + ": Transmitting control command (Control Code: "
//                            + params.controlCode + ")...");
//                    new TransmitTask().execute(params);
//                }
//            }
//        });
//
//        // Initialize get features button
//        mGetFeaturesButton = (Button) findViewById(R.id.main_button_get_features);
//        mGetFeaturesButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                // Get slot number
//                int slotNum = mSlotSpinner.getSelectedItemPosition();
//
//                // If slot is selected
//                if (slotNum != Spinner.INVALID_POSITION) {
//
//                    // Set parameters
//                    TransmitParams params = new TransmitParams();
//                    params.slotNum = slotNum;
//                    params.controlCode = Reader.IOCTL_GET_FEATURE_REQUEST;
//                    params.commandString = "";
//
//                    // Transmit control command
//                    logMsg("Slot " + slotNum
//                            + ": Getting features (Control Code: "
//                            + params.controlCode + ")...");
//                    new TransmitTask().execute(params);
//                }
//            }
//        });
//
//        // PIN verification command (ACOS3)
//        byte[] pinVerifyData = { (byte) 0x80, 0x20, 0x06, 0x00, 0x08,
//                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
//                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
//
//        // Initialize PIN verify structure (ACOS3)
//        mPinVerify.setTimeOut(0);
//        mPinVerify.setTimeOut2(0);
//        mPinVerify.setFormatString(0);
//        mPinVerify.setPinBlockString(0x08);
//        mPinVerify.setPinLengthFormat(0);
//        mPinVerify.setPinMaxExtraDigit(0x0408);
//        mPinVerify.setEntryValidationCondition(0x03);
//        mPinVerify.setNumberMessage(0x01);
//        mPinVerify.setLangId(0x0409);
//        mPinVerify.setMsgIndex(0);
//        mPinVerify.setTeoPrologue(0, 0);
//        mPinVerify.setTeoPrologue(1, 0);
//        mPinVerify.setTeoPrologue(2, 0);
//        mPinVerify.setData(pinVerifyData, pinVerifyData.length);
//
//        // Initialize verify pin button
//        mVerifyPinButton = (Button) findViewById(R.id.main_button_verify_pin);
//        mVerifyPinButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                showDialog(DIALOG_VERIFY_PIN_ID);
//            }
//        });
//
//        // PIN modification command (ACOS3)
//        byte[] pinModifyData = { (byte) 0x80, 0x24, 0x00, 0x00, 0x08,
//                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
//                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
//
//        // Initialize PIN modify structure (ACOS3)
//        mPinModify.setTimeOut(0);
//        mPinModify.setTimeOut2(0);
//        mPinModify.setFormatString(0);
//        mPinModify.setPinBlockString(0x08);
//        mPinModify.setPinLengthFormat(0);
//        mPinModify.setInsertionOffsetOld(0);
//        mPinModify.setInsertionOffsetNew(0);
//        mPinModify.setPinMaxExtraDigit(0x0408);
//        mPinModify.setConfirmPin(0x01);
//        mPinModify.setEntryValidationCondition(0x03);
//        mPinModify.setNumberMessage(0x02);
//        mPinModify.setLangId(0x0409);
//        mPinModify.setMsgIndex1(0);
//        mPinModify.setMsgIndex2(0x01);
//        mPinModify.setMsgIndex3(0);
//        mPinModify.setTeoPrologue(0, 0);
//        mPinModify.setTeoPrologue(1, 0);
//        mPinModify.setTeoPrologue(2, 0);
//        mPinModify.setData(pinModifyData, pinModifyData.length);
//
//        // Initialize modify pin button
//        mModifyPinButton = (Button) findViewById(R.id.main_button_modify_pin);
//        mModifyPinButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                showDialog(DIALOG_MODIFY_PIN_ID);
//            }
//        });
//
//        // Initialize read key option
//        mReadKeyOption.setTimeOut(0);
//        mReadKeyOption.setPinMaxExtraDigit(0x0408);
//        mReadKeyOption.setKeyReturnCondition(0x01);
//        mReadKeyOption.setEchoLcdStartPosition(0);
//        mReadKeyOption.setEchoLcdMode(0x01);
//
//        // Initialize read key button
//        mReadKeyButton = (Button) findViewById(R.id.main_button_read_key);
//        mReadKeyButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                showDialog(DIALOG_READ_KEY_ID);
//            }
//        });
//
//        // Initialize LCD message
//        mLcdMessage = "Hello!";
//
//        // Initialize display LCD message button
//        mDisplayLcdMessageButton = (Button) findViewById(R.id.main_button_display_lcd_message);
//        mDisplayLcdMessageButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                showDialog(DIALOG_DISPLAY_LCD_MESSAGE_ID);
//            }
//        });
//
//        // Disable buttons
//        mCloseButton.setEnabled(false);
//        mSlotSpinner.setEnabled(false);
//        mGetStateButton.setEnabled(false);
//        mPowerSpinner.setEnabled(false);
//        mPowerButton.setEnabled(false);
//        mGetAtrButton.setEnabled(false);
//        mT0CheckBox.setEnabled(false);
//        mT1CheckBox.setEnabled(false);
//        mSetProtocolButton.setEnabled(false);
//        mGetProtocolButton.setEnabled(false);
//        mTransmitButton.setEnabled(false);
//        mControlButton.setEnabled(false);
//        mGetFeaturesButton.setEnabled(false);
//        mVerifyPinButton.setEnabled(false);
//        mModifyPinButton.setEnabled(false);
//        mReadKeyButton.setEnabled(false);
//        mDisplayLcdMessageButton.setEnabled(false);
//
//        // Hide input window
//        getWindow().setSoftInputMode(
//                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//
//
//
//        // LXP Data
//
//        // Initialize spinner
//        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_spinner_item, new String[]{"00","11","01","10"});
//        mDataSpinner = (Spinner) findViewById(R.id.main_spinner_data);
//        mDataSpinner.setAdapter(dataAdapter);
//        mDataSpinner.setSelection(Reader.CARD_WARM_RESET);
//
//        mSetDataButton = (Button) findViewById(R.id.main_button_SetData);
//        mSetDataButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //载入密钥
//                SendMsg("FF 82 00 00 06 FF FF FF FF FF FF");
//                //认证密钥
//                SendMsg("FF 86 00 00 05 01 00 01 60 00");
//
//                //设置数据
//                String data="00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
//                switch (mDataSpinner.getSelectedItemPosition()){
//                    case 0:
//                        data="00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
//                        break;
//                    case 1:
//                        data="11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11";
//                        break;
//                    case 2:
//                        data="01 01 01 01 01 01 01 01 01 01 01 01 01 01 01 01";
//                        break;
//                    case 3:
//                        data="10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10";
//                        break;
//                }
//                SendMsg("FF D6 00 01 10 "+data);
//
//                //SendMsg("FF D6 00 01 10 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11 11");
//            }
//        });
//        mGetDataButton = (Button) findViewById(R.id.main_button_GetData);
//        mGetDataButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //载入密钥
//                SendMsg("FF 82 00 00 06 FF FF FF FF FF FF");
//                //认证密钥
//                SendMsg("FF 86 00 00 05 01 00 01 60 00");
//                //获取数据
//                SendMsg("FF B0 00 01 10");
//            }
//        });
//        mDataIdButton = (Button) findViewById(R.id.main_button_Id);
//        mDataIdButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //获取ID
//                SendMsg("FF CA 00 00 00");
//            }
//        });
//        mShowTv=findViewById(R.id.main_show);
//        mShowTv.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ResultList.clear();
//                mShowTv.setText("");
//            }
//        });
//        mDataCheckBox=findViewById(R.id.main_checkbox);

        CheckDevice();
    }

    public void ThreadHalfSecFun() {
        if(ReadIdDelay >0){
            ReadIdDelay--;
        }
    }


    private void CheckDevice()
    {
        boolean requested = false;

        String deviceName="";
        if(mReaderAdapter.getCount()>0) {
            IsHaveDeviceName=true;
            deviceName = (String) mReaderAdapter.getItem(0);
        }else{
            IsHaveDeviceName=false;
            return;
        }

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
    }

    public void DeviceOpen(Handler handler)
    {
        FrgmHandler=handler;
        new OpenTask().execute(device);
    }

    public void DeviceClose()
    {
        new CloseTask().execute();
    }

    public void FunGetState(){

        // Get slot number
        int slotNum = 0;

        // If slot is selected
        if (slotNum != Spinner.INVALID_POSITION) {

            try {

                // Get state
                logMsg("Slot " + slotNum + ": Getting state...");
                int state = mReader.getState(slotNum);

                if (state < Reader.CARD_UNKNOWN
                        || state > Reader.CARD_SPECIFIC) {
                    state = Reader.CARD_UNKNOWN;
                }

                logMsg("State: " + stateStrings[state]);

            } catch (IllegalArgumentException e) {

                logMsg(e.toString());
                e.printStackTrace();
            }
        }

    }

    public void FunPower(){

        // Get slot number
        int slotNum = 0;

        //"Power Down","Cold Reset", "Warm Reset"
        // Get action number
        int actionNum = 2;

        // If slot and action are selected
        if (slotNum != Spinner.INVALID_POSITION
                && actionNum != Spinner.INVALID_POSITION) {

            if (actionNum < Reader.CARD_POWER_DOWN
                    || actionNum > Reader.CARD_WARM_RESET) {
                actionNum = Reader.CARD_WARM_RESET;
            }

            // Set parameters
            PowerParams params = new PowerParams();
            params.slotNum = slotNum;
            params.action = actionNum;

            // Perform power action
            logMsg("Slot " + slotNum + ": "
                    + powerActionStrings[actionNum] + "...");
            new PowerTask().execute(params);
        }
    }

    public void FunGetAtr()
    {
        // Get slot number
        int slotNum = 0;

        // If slot is selected
        if (slotNum != Spinner.INVALID_POSITION) {

            try {

                // Get ATR
                logMsg("Slot " + slotNum + ": Getting ATR...");
                byte[] atr = mReader.getAtr(slotNum);

                // Show ATR
                if (atr != null) {

                    logMsg("ATR:");
                    logBuffer(atr, atr.length);

                } else {

                    logMsg("ATR: None");
                }

            } catch (IllegalArgumentException e) {

                logMsg(e.toString());
                e.printStackTrace();
            }
        }
    }

    public void FunSetProtocol() {

        // Get slot number
        int slotNum =0;

        // If slot is selected
        if (slotNum != Spinner.INVALID_POSITION) {

            int preferredProtocols = Reader.PROTOCOL_UNDEFINED;
            String preferredProtocolsString = "";

            if (true) {

                preferredProtocols |= Reader.PROTOCOL_T0;
                preferredProtocolsString = "T=0";
            }

            if (true) {
                preferredProtocols |= Reader.PROTOCOL_T1;
                if (preferredProtocolsString != "") {
                    preferredProtocolsString += "/";
                }

                preferredProtocolsString += "T=1";
            }

            if (preferredProtocolsString == "") {
                preferredProtocolsString = "None";
            }

            // Set Parameters
            SetProtocolParams params = new SetProtocolParams();
            params.slotNum = slotNum;
            params.preferredProtocols = preferredProtocols;

            // Set protocol
            logMsg("Slot " + slotNum + ": Setting protocol to "
                    + preferredProtocolsString + "...");
            new SetProtocolTask().execute(params);
        }
    }

    public void FunGetProtocol()
    {

        // Get slot number
        int slotNum = 0;

        // If slot is selected
        if (slotNum != Spinner.INVALID_POSITION) {

            try {

                // Get active protocol
                logMsg("Slot " + slotNum
                        + ": Getting active protocol...");
                int activeProtocol = mReader.getProtocol(slotNum);

                // Show active protocol
                String activeProtocolString = "Active Protocol: ";
                switch (activeProtocol) {

                    case Reader.PROTOCOL_T0:
                        activeProtocolString += "T=0";
                        break;

                    case Reader.PROTOCOL_T1:
                        activeProtocolString += "T=1";
                        break;

                    default:
                        activeProtocolString += "Unknown";
                        break;
                }

                logMsg(activeProtocolString);

            } catch (IllegalArgumentException e) {

                logMsg(e.toString());
                e.printStackTrace();
            }
        }
    }


    public void FunDataId()
    {
        //获取ID
        SendMsg("FF CA 00 00 00");
    }

    public void FunGetData()
    {
        //载入密钥
        SendMsg("FF 82 00 00 06 FF FF FF FF FF FF");
        //认证密钥
        SendMsg("FF 86 00 00 05 01 00 01 60 00");
        //获取数据
        SendMsg("FF B0 00 01 10");
    }

    public void FunSetData(String data)
    {
        //载入密钥
        SendMsg("FF 82 00 00 06 FF FF FF FF FF FF");
        //认证密钥
        SendMsg("FF 86 00 00 05 01 00 01 60 00");

        //设置数据
        SendMsg("FF D6 00 01 10 "+data);
    }


//    public void FunTransmit()
//    {
//        // Get slot number
//        int slotNum = mSlotSpinner.getSelectedItemPosition();
//
//        // If slot is selected
//        if (slotNum != Spinner.INVALID_POSITION) {
//
//            // Set parameters
//            TransmitParams params = new TransmitParams();
//            params.slotNum = slotNum;
//            params.controlCode = -1;
//            params.commandString = mCommandEditText.getText()
//                    .toString();
//
//            // Transmit APDU
//            logMsg("Slot " + slotNum + ": Transmitting APDU...");
//            new TransmitTask().execute(params);
//        }
//    }

//    public void FunControlEdit()
//    {
//        // Get slot number
//        int slotNum =0;
//
//        // If slot is selected
//        if (slotNum != Spinner.INVALID_POSITION) {
//
//            // Get control code
//            int controlCode;
//            try {
//
//                controlCode = Integer.parseInt(mControlEditText
//                        .getText().toString());
//
//            } catch (NumberFormatException e) {
//
//                controlCode = Reader.IOCTL_CCID_ESCAPE;
//            }
//
//            // Set parameters
//            TransmitParams params = new TransmitParams();
//            params.slotNum = slotNum;
//            params.controlCode = controlCode;
//            params.commandString = mCommandEditText.getText()
//                    .toString();
//
//            // Transmit control command
//            logMsg("Slot " + slotNum
//                    + ": Transmitting control command (Control Code: "
//                    + params.controlCode + ")...");
//            new TransmitTask().execute(params);
//        }
//    }



    public  void  SendMsg(String cmd){
        // Get slot number
        int slotNum = 0;

        // If slot is selected
        if (slotNum != Spinner.INVALID_POSITION) {

            // Set parameters
            TransmitParams params = new TransmitParams();
            params.slotNum = slotNum;
            params.controlCode = -1;
            params.commandString = cmd;

            // Transmit APDU
            logMsg("Slot " + slotNum + ": Transmitting APDU...");
            new TransmitTask().execute(params);
        }
    }


    public void onDestroy() {
        // Close reader
        mReader.close();

        // Unregister receiver
        MyContext.unregisterReceiver(mReceiver);
    }


//    protected Dialog onCreateDialog(int id) {
//
//        LayoutInflater inflater;
//        final View layout;
//        AlertDialog.Builder builder;
//        AlertDialog dialog;
//
//        switch (id) {
//
//            case DIALOG_VERIFY_PIN_ID:
//                inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//                layout = inflater
//                        .inflate(
//                                R.layout.verify_pin_dialog,
//                                (ViewGroup) findViewById(R.id.verify_pin_dialog_scroll_view));
//
//                builder = new AlertDialog.Builder(this);
//                builder.setView(layout);
//                builder.setTitle("Verify PIN");
//                builder.setPositiveButton("OK",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                                EditText editText;
//                                byte[] buffer;
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_timeout);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinVerify.setTimeOut(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_timeout2);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinVerify.setTimeOut2(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_format_string);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinVerify.setFormatString(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_pin_block_string);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinVerify.setPinBlockString(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_pin_length_format);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinVerify.setPinLengthFormat(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_pin_max_extra_digit);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 1) {
//                                    mPinVerify
//                                            .setPinMaxExtraDigit((buffer[0] & 0xFF) << 8
//                                                    | (buffer[1] & 0xFF));
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_entry_validation_condition);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinVerify
//                                            .setEntryValidationCondition(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_number_message);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinVerify.setNumberMessage(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_lang_id);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 1) {
//                                    mPinVerify.setLangId((buffer[0] & 0xFF) << 8
//                                            | (buffer[1] & 0xFF));
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_msg_index);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinVerify.setMsgIndex(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_teo_prologue);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 2) {
//                                    mPinVerify.setTeoPrologue(0, buffer[0] & 0xFF);
//                                    mPinVerify.setTeoPrologue(1, buffer[1] & 0xFF);
//                                    mPinVerify.setTeoPrologue(2, buffer[2] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.verify_pin_dialog_edit_text_data);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinVerify.setData(buffer, buffer.length);
//                                }
//
//                                // Get slot number
//                                int slotNum = mSlotSpinner
//                                        .getSelectedItemPosition();
//
//                                // If slot is selected
//                                if (slotNum != Spinner.INVALID_POSITION) {
//
//                                    // Set parameters
//                                    TransmitParams params = new TransmitParams();
//                                    params.slotNum = slotNum;
//                                    params.controlCode = mFeatures
//                                            .getControlCode(Features.FEATURE_VERIFY_PIN_DIRECT);
//                                    params.commandString = toHexString(mPinVerify
//                                            .toByteArray());
//
//                                    // Transmit control command
//                                    logMsg("Slot " + slotNum
//                                            + ": Verifying PIN (Control Code: "
//                                            + params.controlCode + ")...");
//                                    new TransmitTask().execute(params);
//                                }
//
//                                dialog.dismiss();
//                            }
//                        });
//                builder.setNegativeButton("Cancel",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//
//                dialog = builder.create();
//
//                // Hide input window
//                dialog.getWindow().setSoftInputMode(
//                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//                break;
//
//            case DIALOG_MODIFY_PIN_ID:
//                inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//                layout = inflater
//                        .inflate(
//                                R.layout.modify_pin_dialog,
//                                (ViewGroup) findViewById(R.id.modify_pin_dialog_scroll_view));
//
//                builder = new AlertDialog.Builder(this);
//                builder.setView(layout);
//                builder.setTitle("Modify PIN");
//                builder.setPositiveButton("OK",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                                EditText editText;
//                                byte[] buffer;
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_timeout);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setTimeOut(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_timeout2);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setTimeOut2(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_format_string);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setFormatString(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_pin_block_string);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setPinBlockString(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_pin_length_format);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setPinLengthFormat(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_insertion_offset_old);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify
//                                            .setInsertionOffsetOld(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_insertion_offset_new);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify
//                                            .setInsertionOffsetNew(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_pin_max_extra_digit);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 1) {
//                                    mPinModify
//                                            .setPinMaxExtraDigit((buffer[0] & 0xFF) << 8
//                                                    | (buffer[1] & 0xFF));
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_confirm_pin);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setConfirmPin(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_entry_validation_condition);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify
//                                            .setEntryValidationCondition(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_number_message);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setNumberMessage(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_lang_id);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 1) {
//                                    mPinModify.setLangId((buffer[0] & 0xFF) << 8
//                                            | (buffer[1] & 0xFF));
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_msg_index1);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setMsgIndex1(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_msg_index2);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setMsgIndex2(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_msg_index3);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setMsgIndex3(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_teo_prologue);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 2) {
//                                    mPinModify.setTeoPrologue(0, buffer[0] & 0xFF);
//                                    mPinModify.setTeoPrologue(1, buffer[1] & 0xFF);
//                                    mPinModify.setTeoPrologue(2, buffer[2] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.modify_pin_dialog_edit_text_data);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mPinModify.setData(buffer, buffer.length);
//                                }
//
//                                // Get slot number
//                                int slotNum = mSlotSpinner
//                                        .getSelectedItemPosition();
//
//                                // If slot is selected
//                                if (slotNum != Spinner.INVALID_POSITION) {
//
//                                    // Set parameters
//                                    TransmitParams params = new TransmitParams();
//                                    params.slotNum = slotNum;
//                                    params.controlCode = mFeatures
//                                            .getControlCode(Features.FEATURE_MODIFY_PIN_DIRECT);
//                                    params.commandString = toHexString(mPinModify
//                                            .toByteArray());
//
//                                    // Transmit control command
//                                    logMsg("Slot " + slotNum
//                                            + ": Modifying PIN (Control Code: "
//                                            + params.controlCode + ")...");
//                                    new TransmitTask().execute(params);
//                                }
//
//                                dialog.dismiss();
//                            }
//                        });
//                builder.setNegativeButton("Cancel",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//
//                dialog = builder.create();
//
//                // Hide input window
//                dialog.getWindow().setSoftInputMode(
//                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//                break;
//
//            case DIALOG_READ_KEY_ID:
//                inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//                layout = inflater.inflate(R.layout.read_key_dialog,
//                        (ViewGroup) findViewById(R.id.read_key_dialog_scroll_view));
//
//                builder = new AlertDialog.Builder(this);
//                builder.setView(layout);
//                builder.setTitle("Read Key");
//                builder.setPositiveButton("OK",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                                EditText editText;
//                                byte[] buffer;
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.read_key_dialog_edit_text_timeout);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mReadKeyOption.setTimeOut(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.read_key_dialog_edit_text_pin_max_extra_digit);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 1) {
//                                    mReadKeyOption
//                                            .setPinMaxExtraDigit((buffer[0] & 0xFF) << 8
//                                                    | (buffer[1] & 0xFF));
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.read_key_dialog_edit_text_key_return_condition);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mReadKeyOption
//                                            .setKeyReturnCondition(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.read_key_dialog_edit_text_echo_lcd_start_position);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mReadKeyOption
//                                            .setEchoLcdStartPosition(buffer[0] & 0xFF);
//                                }
//
//                                editText = (EditText) layout
//                                        .findViewById(R.id.read_key_dialog_edit_text_echo_lcd_mode);
//                                buffer = toByteArray(editText.getText().toString());
//                                if (buffer != null && buffer.length > 0) {
//                                    mReadKeyOption.setEchoLcdMode(buffer[0] & 0xFF);
//                                }
//
//                                // Get slot number
//                                int slotNum = mSlotSpinner
//                                        .getSelectedItemPosition();
//
//                                // If slot is selected
//                                if (slotNum != Spinner.INVALID_POSITION) {
//
//                                    // Set parameters
//                                    TransmitParams params = new TransmitParams();
//                                    params.slotNum = slotNum;
//                                    params.controlCode = Reader.IOCTL_ACR83_READ_KEY;
//                                    params.commandString = toHexString(mReadKeyOption
//                                            .toByteArray());
//
//                                    // Transmit control command
//                                    logMsg("Slot " + slotNum
//                                            + ": Reading key (Control Code: "
//                                            + params.controlCode + ")...");
//                                    new TransmitTask().execute(params);
//                                }
//
//                                dialog.dismiss();
//                            }
//                        });
//                builder.setNegativeButton("Cancel",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//
//                dialog = builder.create();
//
//                // Hide input window
//                dialog.getWindow().setSoftInputMode(
//                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//                break;
//
//            case DIALOG_DISPLAY_LCD_MESSAGE_ID:
//                inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//                layout = inflater
//                        .inflate(
//                                R.layout.display_lcd_message_dialog,
//                                (ViewGroup) findViewById(R.id.display_lcd_message_dialog_scroll_view));
//
//                builder = new AlertDialog.Builder(this);
//                builder.setView(layout);
//                builder.setTitle("Display LCD Message");
//                builder.setPositiveButton("OK",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                                EditText editText = (EditText) layout
//                                        .findViewById(R.id.display_lcd_message_dialog_edit_text_message);
//                                mLcdMessage = editText.getText().toString();
//
//                                // Get slot number
//                                int slotNum = mSlotSpinner
//                                        .getSelectedItemPosition();
//
//                                // If slot is selected
//                                if (slotNum != Spinner.INVALID_POSITION) {
//
//                                    // Set parameters
//                                    TransmitParams params = new TransmitParams();
//                                    params.slotNum = slotNum;
//                                    params.controlCode = Reader.IOCTL_ACR83_DISPLAY_LCD_MESSAGE;
//                                    try {
//                                        params.commandString = toHexString(mLcdMessage
//                                                .getBytes("US-ASCII"));
//                                    } catch (UnsupportedEncodingException e) {
//                                        params.commandString = "";
//                                    }
//
//                                    // Transmit control command
//                                    logMsg("Slot "
//                                            + slotNum
//                                            + ": Displaying LCD message (Control Code: "
//                                            + params.controlCode + ")...");
//                                    new TransmitTask().execute(params);
//                                }
//
//                                dialog.dismiss();
//                            }
//                        });
//                builder.setNegativeButton("Cancel",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//
//                dialog = builder.create();
//
//                // Hide input window
//                dialog.getWindow().setSoftInputMode(
//                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//                break;
//
//            default:
//                dialog = null;
//                break;
//        }
//
//        return dialog;
//    }
//
//    @Override
//    protected void onPrepareDialog(int id, Dialog dialog) {
//
//        EditText editText;
//
//        switch (id) {
//
//            case DIALOG_VERIFY_PIN_ID:
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_timeout);
//                editText.setText(toHexString(mPinVerify.getTimeOut()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_timeout2);
//                editText.setText(toHexString(mPinVerify.getTimeOut2()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_format_string);
//                editText.setText(toHexString(mPinVerify.getFormatString()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_pin_block_string);
//                editText.setText(toHexString(mPinVerify.getPinBlockString()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_pin_length_format);
//                editText.setText(toHexString(mPinVerify.getPinLengthFormat()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_pin_max_extra_digit);
//                editText.setText(toHexString(mPinVerify.getPinMaxExtraDigit()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_entry_validation_condition);
//                editText.setText(toHexString(mPinVerify
//                        .getEntryValidationCondition()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_number_message);
//                editText.setText(toHexString(mPinVerify.getNumberMessage()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_lang_id);
//                editText.setText(toHexString(mPinVerify.getLangId()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_msg_index);
//                editText.setText(toHexString(mPinVerify.getMsgIndex()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_teo_prologue);
//                editText.setText(toHexString(mPinVerify.getTeoPrologue(0)) + " "
//                        + toHexString(mPinVerify.getTeoPrologue(1)) + " "
//                        + toHexString(mPinVerify.getTeoPrologue(2)));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.verify_pin_dialog_edit_text_data);
//                editText.setText(toHexString(mPinVerify.getData()));
//                break;
//
//            case DIALOG_MODIFY_PIN_ID:
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_timeout);
//                editText.setText(toHexString(mPinModify.getTimeOut()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_timeout2);
//                editText.setText(toHexString(mPinModify.getTimeOut2()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_format_string);
//                editText.setText(toHexString(mPinModify.getFormatString()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_pin_block_string);
//                editText.setText(toHexString(mPinModify.getPinBlockString()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_pin_length_format);
//                editText.setText(toHexString(mPinModify.getPinLengthFormat()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_insertion_offset_new);
//                editText.setText(toHexString(mPinModify.getInsertionOffsetNew()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_insertion_offset_old);
//                editText.setText(toHexString(mPinModify.getInsertionOffsetOld()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_pin_max_extra_digit);
//                editText.setText(toHexString(mPinModify.getPinMaxExtraDigit()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_confirm_pin);
//                editText.setText(toHexString(mPinModify.getConfirmPin()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_entry_validation_condition);
//                editText.setText(toHexString(mPinModify
//                        .getEntryValidationCondition()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_number_message);
//                editText.setText(toHexString(mPinModify.getNumberMessage()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_lang_id);
//                editText.setText(toHexString(mPinModify.getLangId()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_msg_index1);
//                editText.setText(toHexString(mPinModify.getMsgIndex1()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_msg_index2);
//                editText.setText(toHexString(mPinModify.getMsgIndex2()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_msg_index3);
//                editText.setText(toHexString(mPinModify.getMsgIndex3()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_teo_prologue);
//                editText.setText(toHexString(mPinModify.getTeoPrologue(0)) + " "
//                        + toHexString(mPinModify.getTeoPrologue(1)) + " "
//                        + toHexString(mPinModify.getTeoPrologue(2)));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.modify_pin_dialog_edit_text_data);
//                editText.setText(toHexString(mPinModify.getData()));
//                break;
//
//            case DIALOG_READ_KEY_ID:
//                editText = (EditText) dialog
//                        .findViewById(R.id.read_key_dialog_edit_text_timeout);
//                editText.setText(toHexString(mReadKeyOption.getTimeOut()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.read_key_dialog_edit_text_pin_max_extra_digit);
//                editText.setText(toHexString(mReadKeyOption.getPinMaxExtraDigit()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.read_key_dialog_edit_text_key_return_condition);
//                editText.setText(toHexString(mReadKeyOption.getKeyReturnCondition()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.read_key_dialog_edit_text_echo_lcd_start_position);
//                editText.setText(toHexString(mReadKeyOption
//                        .getEchoLcdStartPosition()));
//
//                editText = (EditText) dialog
//                        .findViewById(R.id.read_key_dialog_edit_text_echo_lcd_mode);
//                editText.setText(toHexString(mReadKeyOption.getEchoLcdMode()));
//                break;
//
//            case DIALOG_DISPLAY_LCD_MESSAGE_ID:
//                editText = (EditText) dialog
//                        .findViewById(R.id.display_lcd_message_dialog_edit_text_message);
//                editText.setText(mLcdMessage);
//                break;
//
//            default:
//                break;
//        }
//    }

    /**
     * Logs the message.
     *
     * @param msg
     *            the message.
     */
    private void logMsg(String msg)
    {
        //DateFormat dateFormat = new SimpleDateFormat("[dd-MM-yyyy HH:mm:ss]: ");
        //Date date = new Date();
        //String oldMsg = mResponseTextView.getText().toString();

        Log.v(TAG,"logMsg "+msg);
        String data=msg.trim().toLowerCase();
        //检测到插卡 发送查看卡号命令

        //有新的卡放置
        if (data.indexOf("-> present") != -1) {
            if (ReadIdDelay <= 0) {
                ReadIdDelay = 2;

                FunGetState();
                FunPower();
                FunSetProtocol();
                FunDataId();
                FunGetData();
            }
            //NFCTestActivity.activity.ShowType("检测到标签");
        }
        //卡拿开
        if (data.indexOf("-> absent") != -1) {
            ReadIdDelay = 0;
            //NFCTestActivity.activity.ShowType("当前无标签");
        }

        if(LastCmdState ==1){
            if(data.indexOf("ff ca 00")!=-1){
                ResultList.add("Com:查询卡号");

                Log.v(TAG,"Type_ID");
                LastCmdType=Type_ID;
            }else if(data.indexOf("ff 82 00 00 06")!=-1){
                ResultList.add("Com:认证");
            }else if(data.indexOf("ff 86 00 00 05")!=-1){
                ResultList.add("Com:密钥");
            }else if(data.indexOf("ff d6 00")!=-1){
                ResultList.add("Com:写数据");
                ResultList.add("Com:"+msg);

                LastCmdType=Type_SetData;
            }else if(data.indexOf("ff b0 00")!=-1){
                ResultList.add("Com:读数据");

                Log.v(TAG,"Type_GetData");
                LastCmdType=Type_GetData;
            }else{
                ResultList.add("Com:"+msg);
            }
        }else if(LastCmdState ==2){
            if(data.equals("90 00")){
                LastCmdSuccess=true;
                ResultList.add("Rsp:指令完成");

                switch (LastCmdType){
                    case Type_SetData:
                        //NFCTestActivity.activity.ShowType("写数据成功");

                        //重新读取
                        FunGetData();
                        break;
                }
            }else if(data.equals("63 00")){
                LastCmdSuccess=false;
                ResultList.add("Rsp:指令失败");

                switch (LastCmdType){
                    case Type_SetData:
                        //NFCTestActivity.activity.ShowType("写数据失败");
                        break;
                    case Type_GetData:
                        //NFCTestActivity.activity.ShowType("读数据失败");
                        break;
                }
            }else{
                switch (LastCmdType){
                    case Type_ID:
                        ResultList.add("Rsp:"+msg);

                        //NFCTestActivity.activity.ShowId("ID\r\n"+msg);
                        Message message=FrgmHandler.obtainMessage();
                        message.what= MainActivity.MsgID_HW_NFCID;
                        message.obj=msg;
                        FrgmHandler.sendMessage(message);
                        break;
                    case Type_SetData:
                        ResultList.add("Set Rsp:"+msg);
                        break;
                    case Type_GetData:
                        ResultList.add("Get Rsp:"+msg);

                        //NFCTestActivity.activity.ShowInfo("lxp:"+msg);
                        //CTestActivity.activity.ShowType("读数据成功");
                        break;
                    default:
                        ResultList.add(LastCmdType+" Rsp:"+msg);
                        break;
                }
            }
            LastCmdType=Type_Null;
            Log.v(TAG,"Type_Null");
        }

        //准备获取下一条
        if(msg.indexOf("Comm")!=-1){
            LastCmdState =1;
        }else if(msg.indexOf("Resp")!=-1){
            LastCmdState =2;
        }else{
            LastCmdState =0;
        }

        data="";
        for (int i = 0; i <ResultList.size(); i++) {
            data+=ResultList.get(i)+"\r\n";
        }


//        mResponseTextView
//                .setText(oldMsg + "\n" + dateFormat.format(date) + msg);
//
//        if (mResponseTextView.getLineCount() > MAX_LINES) {
//            mResponseTextView.scrollTo(0,
//                    (mResponseTextView.getLineCount() - MAX_LINES)
//                            * mResponseTextView.getLineHeight());
//        }
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

    /**
     * Logs the contents of buffer.
     *
     * @param buffer
     *            the buffer.
     * @param bufferLength
     *            the buffer length.
     */
    private String GetlogBuffer(byte[] buffer, int bufferLength) {

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
}
