package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity
        implements NfcAdapter.OnNdefPushCompleteCallback,
        NfcAdapter.CreateNdefMessageCallback {
    //The array lists to hold our messages
    private ArrayList<String> messagesToSendArray = new ArrayList<>();
    private ArrayList<String> messagesReceivedArray = new ArrayList<>();

    //Text boxes to add and display our messages
    private EditText txtBoxAddMessage;
    private TextView txtReceivedMessages;
    private TextView txtMessagesToSend;

    private NfcAdapter mNfcAdapter;

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public void addMessage(View view) {
        String newMessage = txtBoxAddMessage.getText().toString();
        messagesToSendArray.add(newMessage);

        txtBoxAddMessage.setText(null);
        updateTextViews();

        Toast.makeText(this, "Added Message", Toast.LENGTH_LONG).show();
    }

    private void updateTextViews() {
        txtMessagesToSend.setText("Messages To Send:\n");
        //Populate Our list of messages we want to send
        if(messagesToSendArray.size() > 0) {
            for (int i = 0; i < messagesToSendArray.size(); i++) {
                txtMessagesToSend.append(messagesToSendArray.get(i));
                txtMessagesToSend.append("\n");
            }
        }
        txtMessagesToSend.setMovementMethod(new ScrollingMovementMethod());
        txtReceivedMessages.setText("Messages Received:\n");
        //Populate our list of messages we have received
        if (messagesReceivedArray.size() > 0) {
            for (int i = 0; i < messagesReceivedArray.size(); i++) {
                txtReceivedMessages.append(messagesReceivedArray.get(i));
                txtReceivedMessages.append("\n");
            }
        }
        txtReceivedMessages.setMovementMethod(new ScrollingMovementMethod());
    }

    //Save our Array Lists of Messages for if the user navigates away
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putStringArrayList("messagesToSend", messagesToSendArray);
        savedInstanceState.putStringArrayList("lastMessagesReceived",messagesReceivedArray);
    }

    //Load our Array Lists of Messages for when the user navigates back
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        messagesToSendArray = savedInstanceState.getStringArrayList("messagesToSend");
        messagesReceivedArray = savedInstanceState.getStringArrayList("lastMessagesReceived");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nfc);

        txtBoxAddMessage = findViewById(R.id.txtBoxAddMessage);
        txtMessagesToSend = findViewById(R.id.txtMessageToSend);
        txtReceivedMessages = findViewById(R.id.txtMessagesReceived);
        Button btnAddMessage = findViewById(R.id.buttonAddMessage);

        btnAddMessage.setText("Add Message");
        updateTextViews();

        //Check if NFC is available on device
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(mNfcAdapter != null) {
            //This will refer back to createNdefMessage for what it will send
            mNfcAdapter.setNdefPushMessageCallback(this, this);

            //This will be called if the message is sent successfully
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);

            if (!mNfcAdapter.isEnabled()) {
                Toast.makeText(this, "NFC is disabled.",
                        Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "NFC not available on this device",
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_nfc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear:
                messagesReceivedArray = new ArrayList<>();
                updateTextViews();
                break;
            case R.id.menu_switch:
                Intent intent = new Intent(this, DeviceScanActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        //This is called when the system detects that our NdefMessage was
        //Successfully sent.
        messagesToSendArray.clear();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        //This will be called when another NFC capable device is detected.
        if (messagesToSendArray.size() == 0) {
            return null;
        }
        //We'll write the createRecords() method in just a moment
        NdefRecord[] recordsToAttach = createRecords();
        //When creating an NdefMessage we need to provide an NdefRecord[]
        return new NdefMessage(recordsToAttach);
    }

    public NdefRecord[] createRecords() {
        NdefRecord[] records = new NdefRecord[messagesToSendArray.size() + 1];
        //To Create Messages Manually if API is less than
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            for (int i = 0; i < messagesToSendArray.size(); i++){
                byte[] payload = messagesToSendArray.get(i).
                        getBytes(Charset.forName("UTF-8"));
                NdefRecord record = new NdefRecord(
                        NdefRecord.TNF_WELL_KNOWN,      //Our 3-bit Type name format
                        NdefRecord.RTD_TEXT,            //Description of our payload
                        new byte[0],                    //The optional id for our Record
                        payload);                       //Our payload for the Record
                records[i] = record;
            }
        }
        //Api is high enough that we can use createMime, which is preferred.
        else {
            for (int i = 0; i < messagesToSendArray.size(); i++){
                byte[] payload = messagesToSendArray.get(i).
                        getBytes(Charset.forName("UTF-8"));
                NdefRecord record = NdefRecord.createMime("text/plain",payload);
                records[i] = record;
            }
        }
        records[messagesToSendArray.size()] =
                NdefRecord.createApplicationRecord(getPackageName());
        return records;
    }

    private void handleNfcIntent(Intent NfcIntent) {
        //checks for the ACTION_NDEF_DISCOVERED intent and gets the NDEF messages from an intent extra.
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(NfcIntent.getAction())) {
            Parcelable[] receivedArray =
                    NfcIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if(receivedArray != null) {
                NdefMessage receivedMessage = (NdefMessage) receivedArray[0];
                NdefRecord[] attachedRecords = receivedMessage.getRecords();
                for (NdefRecord record:attachedRecords) {
                    String string = new String(record.getPayload());
                    //Make sure we don't pass along our AAR (Android Application Record)
                    if (string.equals(getPackageName())) {
                        continue;
                    }
                    messagesReceivedArray.add(string);
                }
                Toast.makeText(this, "Received " + messagesReceivedArray.size() +
                        " Messages", Toast.LENGTH_LONG).show();
                updateTextViews();
            }
            else {
                Toast.makeText(this, "Received Blank Parcel", Toast.LENGTH_LONG).show();
            }
        }
        else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(NfcIntent.getAction())) {
            Tag tagFromIntent = NfcIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techlist = tagFromIntent.getTechList();
            if (techlist != null) {
                for (String tech:techlist) {
                    messagesReceivedArray.add(tech);
                }
                Toast.makeText(this, "Received " + messagesReceivedArray.size() +
                        " Messages", Toast.LENGTH_LONG).show();
                messagesReceivedArray.add("\n");
                updateTextViews();
            }
            else {
                Toast.makeText(this, "Received Blank Parcel", Toast.LENGTH_LONG).show();
            }
        }
        else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(NfcIntent.getAction())) {
            Tag tagFromIntent = NfcIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            HashMap<String, String> tagDataHashMap = handleTag(tagFromIntent);
            if (tagDataHashMap.size() != 0) {
                for (String tech:tagDataHashMap.keySet()) {
                    String toAdd = tech + ": " + tagDataHashMap.get(tech);
                    messagesReceivedArray.add(toAdd);
                }
                //Toast.makeText(this, "Received " + messagesReceivedArray.size() + " Messages", Toast.LENGTH_LONG).show();
                messagesReceivedArray.add("\n");
                updateTextViews();
            }
            else {
                Toast.makeText(this, "Received Blank Parcel", Toast.LENGTH_LONG).show();
            }
        }
    }

    private HashMap<String, String> handleTag(Tag tag) {
        String[] techlist = tag.getTechList();
        HashMap<String, String> info = new HashMap<>();

        byte[] id = tag.getId();
        String idString = bytesToHex(id);
        String output = idString.replaceAll("..(?!$)", "$0:");

        info.put("Serial Number", output);

        for (int i = 0; i < techlist.length; i++) {
            if (techlist[i].equals(NfcA.class.getName())) {
                NfcA a = NfcA.get(tag);
                if (info.containsKey("Tag Technology")) {
                    info.put("Tag Technology", info.get("Tag Technology") + ", NfcA");
                }
                else {
                    info.put("Tag Technology", "NfcA");
                }
                StringBuilder AQTAValue = new StringBuilder();
                AQTAValue.append(bytesToHex(a.getAtqa()));
                while (AQTAValue.length() < 4) {
                    AQTAValue.append("0");// + AQTAValue;
                }
                AQTAValue.append("x0");
                AQTAValue = AQTAValue.reverse();
                info.put("AQTA", AQTAValue.toString());

                String SAKValue = String.valueOf(a.getSak());
                while (SAKValue.length() < 2) {
                    SAKValue = "0" + SAKValue;
                }
                SAKValue = "0x" + SAKValue;
                info.put("SAK",SAKValue);

                String maxTransceiveLength = String.valueOf(a.getMaxTransceiveLength());
                info.put("Maximum bytes that can be sent ", maxTransceiveLength);
                String timeout = String.valueOf(a.getTimeout());
                info.put("Timeout", timeout + " ms");
                String connected = String.valueOf(a.isConnected());
                info.put("Connected", connected);

                if (id.length == 4) {
                    if (idString.substring(0,2).equals("08")) {
                        info.put("ID Type", "Random ID");
                    }
                    else {
                        info.put("ID Type", "Unique ID");
                    }
                }
                else {
                    info.put("ID Type", "Unique ID");
                }
            }
            else if (techlist[i].equals(NfcB.class.getName())) {
                NfcB b = NfcB.get(tag);
                if (info.containsKey("Tag Technology")) {
                    info.put("Tag Technology", info.get("Tag Technology") + ", NfcB");
                }
                else {
                    info.put("Tag Technology", "NfcB");
                }
                String maxTransceiveLength = String.valueOf(b.getMaxTransceiveLength());
                info.put("Maximum bytes that can be sent ", maxTransceiveLength);

                String connected = String.valueOf(b.isConnected());
                info.put("Connected", connected);

                String appData = bytesToHex(b.getApplicationData());
                info.put("Application Data", appData);

                String protocolInfo = bytesToHex(b.getProtocolInfo());
                info.put("Protocol Info", protocolInfo);

                info.put("ID Type", "Pseudo-Unique PICC Identifier");
            }
            else if (techlist[i].equals(IsoDep.class.getName())) {
                IsoDep isoDep = IsoDep.get(tag);
                if (info.containsKey("Tag Technology")) {
                    info.put("Tag Technology", info.get("Tag Technology") + ", isoDep");
                }
                else {
                    info.put("Tag Technology", "isoDep");
                }
                String timeout = String.valueOf(isoDep.getTimeout());
                info.put("Timeout", timeout + " ms");
                String maxTransceiveLength = String.valueOf(isoDep.getMaxTransceiveLength());
                info.put("Maximum bytes that can be sent ", maxTransceiveLength);
            }
            else if (techlist[i].equals(MifareClassic.class.getName())) {
                MifareClassic mifareClassic = MifareClassic.get(tag);
                if (info.containsKey("Tag Technology")) {
                    info.put("Tag Technology", info.get("Tag Technology") + ", MifareClassic");
                }
                else {
                    info.put("Tag Technology", "MifareClassic");
                }
                String timeout = String.valueOf(mifareClassic.getTimeout());
                info.put("Timeout", timeout + " ms");
                String maxTransceiveLength = String.valueOf(mifareClassic.getMaxTransceiveLength());
                info.put("Maximum bytes that can be sent ", maxTransceiveLength);
                String blockCount = String.valueOf(mifareClassic.getBlockCount());
                info.put("Block Count ", blockCount);
                String sectorCount = String.valueOf(mifareClassic.getSectorCount());
                info.put("Sector Count ", sectorCount);

                int tagSize = mifareClassic.getSize();
                if (tagSize == MifareClassic.SIZE_1K) {
                    info.put("Tag Size ", "Contains 16 sectors, each with 4 blocks");
                }
                else if (tagSize == MifareClassic.SIZE_2K) {
                    info.put("Tag Size ", "Contains 32 sectors, each with 4 blocks");
                }
                else if (tagSize == MifareClassic.SIZE_2K) {
                    info.put("Tag Size ", "Contains 40 sectors. The first 32 sectors contain 4 blocks and the last 8 sectors contain 16 blocks");
                }
                else if (tagSize == MifareClassic.SIZE_MINI) {
                    info.put("Tag Size ", "Contains 5 sectors, each with 4 blocks");
                }

                int mifareType = mifareClassic.getType();
                if (mifareType == MifareClassic.TYPE_CLASSIC) {
                    info.put("Tag Type ", "MIFARE Classic");
                }
                else if (mifareType == MifareClassic.TYPE_PLUS) {
                    info.put("Tag Type ", "MIFARE Plus");
                }
                else if (mifareType == MifareClassic.TYPE_PRO) {
                    info.put("Tag Type ", "MIFARE Pro");
                }
                else if (mifareType == MifareClassic.TYPE_UNKNOWN) {
                    info.put("Tag Type ", "Unknown");
                }
                if (id.length == 4) {
                    if (idString.substring(0,2).equals("08")) {
                        info.put("ID Type", "Random ID");
                    }
                    else {
                        info.put("ID Type", "Unique ID");
                    }
                }
                else {
                    info.put("ID Type", "Unique ID");
                }
            }
            else if (techlist[i].equals(NfcF.class.getName())) {
                NfcF f = NfcF.get(tag);
                if (info.containsKey("Tag Technology")) {
                    info.put("Tag Technology", info.get("Tag Technology") + ", NfcF");
                }
                else {
                    info.put("Tag Technology", "NfcF");
                }
                String maxTransceiveLength = String.valueOf(f.getMaxTransceiveLength());
                info.put("Maximum bytes that can be sent ", maxTransceiveLength);

                String connected = String.valueOf(f.isConnected());
                info.put("Connected", connected);

                String manufacturerData = bytesToHex(f.getManufacturer());
                info.put("Manufacturer Data", manufacturerData);

                String systemCode = bytesToHex(f.getSystemCode());
                info.put("System Code", systemCode);

                String timeout = String.valueOf(f.getTimeout());
                info.put("Timeout", timeout + " ms");

                info.put("ID Type", "Random ID");
            }
            else if (techlist[i].equals(NfcV.class.getName())) {
                NfcV v = NfcV.get(tag);
                if (info.containsKey("Tag Technology")) {
                    info.put("Tag Technology", info.get("Tag Technology") + ", NfcV");
                }
                else {
                    info.put("Tag Technology", "NfcV");
                }
                String maxTransceiveLength = String.valueOf(v.getMaxTransceiveLength());
                info.put("Maximum bytes that can be sent ", maxTransceiveLength);

                String connected = String.valueOf(v.isConnected());
                info.put("Connected", connected);

                String manufacturerData = Byte.toString(v.getDsfId());
                info.put("Manufacturer Data", manufacturerData);

                String responseFlags = Byte.toString(v.getResponseFlags());
                info.put("Response Flags", responseFlags);

                info.put("ID Type", "Random ID");
            }
        }

        return info;
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNfcIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTextViews();
        handleNfcIntent(getIntent());
    }
}
