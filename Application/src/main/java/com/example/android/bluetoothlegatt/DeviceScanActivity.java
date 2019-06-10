/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mScanning;
    private Handler mHandler;
    private HashMap<String, String> BTCompanyIdentifierHash = new HashMap<>();
    private HashMap<String, String> GAPHash = new HashMap<>();
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 20 seconds.
    private static final long SCAN_PERIOD = 20000;

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } /*else {
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }*/
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.bluetoothcompanyidentifiers);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

            String line;

            while ((line = br.readLine()) != null) {
                String str[] = line.split(",");
                if (str.length < 3) {
                    //Ignore IDs with no company
                    continue;
                }
                for (int i = 3; i < str.length; i++) {
                    //Concatenate strings of the company name if they're split
                    str[2] += str[i];
                }
                if (str[2].substring(0,1).equals("\"")) {
                    //Remove "" if they are at the start and end of the string
                    str[2] = str[2].substring(1, str[2].length() - 1);
                }
                for (int i = 0; i < str.length; i++) {
                    BTCompanyIdentifierHash.put(str[1], str[2]);
                }
            }
            inputStream.close();
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            InputStream inputStream = getResources().openRawResource(R.raw.gap);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

            String line;

            while ((line = br.readLine()) != null) {
                String str[] = line.split(",");
                for (int i = 3; i < str.length; i++) {
                    //Concatenate strings if they're split
                    str[2] += str[i];
                }
                if (str[2].substring(0,1).equals("\"")) {
                    //Remove "" if they are at the start and end of the string
                    str[2] = str[2].substring(1, str[2].length() - 1);
                }
                for (int i = 0; i < str.length; i++) {
                    GAPHash.put(str[0], str[1] + ", " + str[2]);
                }
            }
            inputStream.close();
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mLeDeviceListAdapter == null) {
            mLeDeviceListAdapter = new LeDeviceListAdapter();
            setListAdapter(mLeDeviceListAdapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_switch).setVisible(true);
            //menu.findItem(R.id.menu_refresh).setActionView(null);
            getActionBar().setTitle(R.string.title_devices);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_switch).setVisible(false);
            //menu.findItem(R.id.menu_refresh).setActionView(
            //        R.layout.actionbar_indeterminate_progress);
            getActionBar().setTitle("Scanning");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                scanLeDevice(false);
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.menu_switch:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        // Initializes list view adapter.
        if (mLeDeviceListAdapter == null) {
            Toast.makeText(this, "???", Toast.LENGTH_SHORT).show();

            mLeDeviceListAdapter = new LeDeviceListAdapter();
            setListAdapter(mLeDeviceListAdapter);
        }
        //scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mLeDeviceListAdapter.resetTime();
            mBluetoothLeScanner.startScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private class ScanDevice {
        private BluetoothDevice device;
        private ScanRecord scanRecord;
        private List<UUID> uuidList;
        private Integer rssi;
        private Integer count;
        private ArrayList<Long> timestampNanos = new ArrayList<>();
        private Timestamp initialTime;
        private long calculatedInterval;
        private boolean legacy;
        private int advFlags;

        public BluetoothDevice getDevice() {
            return device;
        }

        public ScanRecord getScanRecord() {
            return scanRecord;
        }

        public List<UUID> getUuidList() {
            return uuidList;
        }

        public Integer getRssi() {
            return rssi;
        }

        public Integer getCount() {
            return count;
        }

        public Timestamp getInitialTime() {
            return initialTime;
        }

        public ArrayList<Long> getTimestampNanos() {
            return timestampNanos;
        }

        public long getCalculatedInterval() {
            return calculatedInterval;
        }

        public void setDevice(BluetoothDevice device) {
            this.device = device;
        }

        public void setScanRecord(ScanRecord scanRecord) {
            this.scanRecord = scanRecord;
        }

        public void setUuidList(List<UUID> uuidList) {
            this.uuidList = uuidList;
        }

        public void setRssi(Integer rssi) {
            this.rssi = rssi;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public void setTimestampNanos(Long timestampNanos) {
            this.timestampNanos.add(timestampNanos);
        }

        public void setTimestampNanosList(ArrayList<Long> timestampNanosList) {
            this.timestampNanos = timestampNanosList;
        }

        public void setInitialTime(Timestamp time) {
            initialTime = time;
        }

        public void setCalculatedInterval(long calculatedInterval) {
            this.calculatedInterval = calculatedInterval;
        }

        public boolean isLegacy() {
            return legacy;
        }

        public void setLegacy(boolean legacy) {
            this.legacy = legacy;
        }

        public int getAdvFlags() {
            return advFlags;
        }

        public void setAdvFlags(int advFlags) {
            this.advFlags = advFlags;
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<ScanDevice> scanDeviceList;
        private LayoutInflater mInflator;
        Timestamp initialTime;

        public LeDeviceListAdapter() {
            super();
            scanDeviceList = new ArrayList<>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public Comparator<ScanDevice> scanDeviceComparatorAddress = new Comparator<ScanDevice>() {
            @Override
            public int compare(ScanDevice scanDevice, ScanDevice other) {
                String address1 = scanDevice.getDevice().getAddress();
                String address2 = other.getDevice().getAddress();
                //ascending order
                return address1.compareTo(address2);
            }
        };

        public void resetTime() {
            initialTime = new Timestamp(System.currentTimeMillis());
        }

        public void addScanDevice(ScanDevice obj) {
            for (int i = 0; i < scanDeviceList.size(); i++){
                if (obj.getDevice().getAddress().equals(scanDeviceList.get(i).getDevice().getAddress())) {
                    obj.setCount(obj.getCount() + scanDeviceList.get(i).getCount());
                    ArrayList<Long> newList = new ArrayList<>(scanDeviceList.get(i).getTimestampNanos());
                    newList.addAll(obj.getTimestampNanos());
                    obj.setTimestampNanosList(newList);
                    obj.setInitialTime(scanDeviceList.get(i).getInitialTime());
                    obj.setCalculatedInterval(calcTimestampNanos(newList));
                    scanDeviceList.set(i, obj);
                    Collections.sort(scanDeviceList, scanDeviceComparatorAddress);

                    return;
                }
            }
            scanDeviceList.add(obj);
            Collections.sort(scanDeviceList, scanDeviceComparatorAddress);
        }

        /**
         * Sets the advertising interval for this kind of packets if the current one is higher than the new one.
         */
        public long calcTimestampNanos(ArrayList<Long> tsnList) {
            Collections.sort(tsnList);
            long result = 0L;
            int count = 0;
            Log.e("Start", "Start");

            for (int i = 0; i < tsnList.size(); i++) {
                count++;
                long intervalNanos;
                if (i > 0) {
                    intervalNanos = tsnList.get(i) - tsnList.get(i-1);
                } else {
                    continue;
                }
                Log.e("current", String.valueOf(result));
                Log.e("next", String.valueOf(intervalNanos));

                if (intervalNanos <= 0L)
                    continue;
                if (result == 0L)
                    result = intervalNanos;
                else if (intervalNanos < result * 0.7 && count < 10)
                    result = intervalNanos;
                else if (intervalNanos < result + 3000000) {
                    final int limitedCount = Math.min(count, 10);
                    result = (result * (limitedCount - 1) + intervalNanos) / limitedCount;
                } else if (intervalNanos < result * 1.4) {
                    result = (result * (29) + intervalNanos) / 30;
                }
            }
            return result;
        }

        public BluetoothDevice getDevice(int position) {
            return scanDeviceList.get(position).getDevice();
        }

        public void clear() {
            scanDeviceList.clear();
        }

        @Override
        public int getCount() {
            return scanDeviceList.size();
        }

        @Override
        public Object getItem(int i) {
            return scanDeviceList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.listitemDevicesLayout = view.findViewById(R.id.listitem_deviceLayout);
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                viewHolder.deviceBluetoothClass = view.findViewById(R.id.device_bluetoothClass);
                viewHolder.deviceLegacy = view.findViewById(R.id.device_legacy);
                viewHolder.deviceAdvFlags = view.findViewById(R.id.device_advFlags);
                viewHolder.deviceBondState = view.findViewById(R.id.device_bondState);
                viewHolder.deviceType = view.findViewById(R.id.device_type);
                viewHolder.deviceManufacturer = view.findViewById(R.id.device_manufacturer);
                viewHolder.deviceRSSI = view.findViewById(R.id.device_rssi);
                viewHolder.deviceHashCode = view.findViewById(R.id.device_hashCode);
                viewHolder.devicePacketsReceived = view.findViewById(R.id.device_packetsReceived);

                viewHolder.deviceAdvertisingInterval = view.findViewById(R.id.device_advertisingInterval);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            if (i >= scanDeviceList.size()) {
                return view;
            }
            if (i%2 == 0) {
                viewHolder.listitemDevicesLayout.setBackgroundColor(getResources().getColor(R.color.LightCyan));
            }
            else {
                viewHolder.listitemDevicesLayout.setBackgroundColor(getResources().getColor(R.color.GhostWhite));
            }
            BluetoothDevice device = scanDeviceList.get(i).getDevice();
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            String macAddress = device.getAddress();
            String[] macAddressParts = macAddress.split(":");
            // convert hex string to binary values
            String binString = "";
            for(int j=0; j<6; j++){
                Integer hex = Integer.parseInt(macAddressParts[j], 16);
                String toBin = Integer.toBinaryString(hex);
                while (toBin.length() < 8) {
                    toBin = "0" + toBin;
                }
                binString += toBin + " ";
            }

            String address = "MAC Address: " + device.getAddress() + "\n" + binString;
            String msb = binString.substring(0, 2);
            if (msb.equals("11")) {
                address += "\n" + "Random Static Address";
                viewHolder.deviceAddress.setTextColor(getResources().getColor(R.color.OrangeRed));
            }
            else if (msb.equals("01")) {
                address += "\n" + "Resolvable Private Address";
                viewHolder.deviceAddress.setTextColor(getResources().getColor(R.color.DarkGoldenrod));
            }
            else if (msb.equals("00")) {
                address += "\n" + "Non-Resolvable Private Address";
                viewHolder.deviceAddress.setTextColor(getResources().getColor(R.color.Green));
            }
            else {
                address += "\n" + "Public Address";
                viewHolder.deviceAddress.setTextColor(getResources().getColor(R.color.DarkRed));
            }
            viewHolder.deviceAddress.setText(address);

            final BluetoothClass bluetoothclass = device.getBluetoothClass();

            if (bluetoothclass != null) {
                int deviceClassValue = device.getBluetoothClass().getDeviceClass();
                if (deviceClassValue == BluetoothClass.Device.Major.UNCATEGORIZED) {
                    viewHolder.deviceBluetoothClass.setText("Uncategorised Bluetooth Device Class");
                }
                else if (deviceClassValue == BluetoothClass.Device.Major.MISC) {
                    viewHolder.deviceBluetoothClass.setText("Misc Bluetooth Device Class");
                }
                else if (deviceClassValue == BluetoothClass.Device.WEARABLE_WRIST_WATCH) {
                    viewHolder.deviceBluetoothClass.setText("Wearable Wrist Watch");
                }
                else if (deviceClassValue == 5460) {
                    viewHolder.deviceBluetoothClass.setText("Windows 10 Desktop");
                }
                else if (deviceClassValue == 120) {
                    viewHolder.deviceBluetoothClass.setText("Apple Device");
                }
                else {
                    viewHolder.deviceBluetoothClass.setText(String.valueOf(deviceClassValue));
                }
            }
            else
                viewHolder.deviceBluetoothClass.setText(R.string.unknown_bluetooth_class);

            String toAddLegacy = "Advertising Type: ";
            if  (scanDeviceList.get(i).isLegacy()) {
                toAddLegacy += "Legacy";
            } else {
                toAddLegacy += "Extended";
            }
            viewHolder.deviceLegacy.setText(toAddLegacy);

            String toAddAdvFlags = "Advertising Flags: ";

            int advFlags = scanDeviceList.get(i).getAdvFlags();
            if (advFlags == -1) {
                toAddAdvFlags += "None";
            } else {
                String hex = Integer.toHexString(advFlags).toUpperCase();
                if (hex.length() < 2) {
                    hex = "0" + hex;
                }
                // convert hex string to binary values
                String flagBin = "";

                String toBin = Integer.toBinaryString(Integer.parseInt(hex.substring(0, 1), 16));
                while (toBin.length() < 4) {
                    toBin = "0" + toBin;
                }
                flagBin += toBin;

                toBin = Integer.toBinaryString(Integer.parseInt(hex.substring(1), 16));
                while (toBin.length() < 4) {
                    toBin = "0" + toBin;
                }
                flagBin += toBin;

                if (flagBin.substring(3,4).equals("1")) {
                    toAddAdvFlags += "LE and BR/EDR Capable (Host), ";
                }
                if (flagBin.substring(4,5).equals("1")) {
                    toAddAdvFlags += "LE and BR/EDR Capable (Controller), ";
                }
                if (flagBin.substring(5,6).equals("1")) {
                    toAddAdvFlags += "BR/EDR Not Supported, ";
                }
                if (flagBin.substring(6,7).equals("1")) {
                    toAddAdvFlags += "LE General Discoverable Mode, ";
                }
                if (flagBin.substring(7,8).equals("1")) {
                    toAddAdvFlags += "LE Limited Discoverable Mode, ";
                }
                toAddAdvFlags = toAddAdvFlags.substring(0, toAddAdvFlags.length() - 2);
            }

            viewHolder.deviceAdvFlags.setText(toAddAdvFlags);

            final int deviceBond = device.getBondState();
            if (deviceBond == BluetoothDevice.BOND_BONDED)
                viewHolder.deviceBondState.setText(R.string.bonded);
            else if (deviceBond == BluetoothDevice.BOND_BONDING)
                viewHolder.deviceBondState.setText(R.string.bonding);
            else if (deviceBond == BluetoothDevice.BOND_NONE)
                viewHolder.deviceBondState.setText(R.string.not_bonded);
            else
                viewHolder.deviceBondState.setText(R.string.unknown_bond);

            final int deviceType = device.getType();
            if (deviceType == BluetoothDevice.DEVICE_TYPE_CLASSIC)
                viewHolder.deviceType.setText(R.string.device_classic);
            else if (deviceType == BluetoothDevice.DEVICE_TYPE_DUAL)
                viewHolder.deviceType.setText(R.string.device_dual);
            else if (deviceType == BluetoothDevice.DEVICE_TYPE_LE)
                viewHolder.deviceType.setText(R.string.device_le);
            else if (deviceType == BluetoothDevice.DEVICE_TYPE_UNKNOWN)
                viewHolder.deviceType.setText(R.string.device_unknown_type);

            String toAddHash = "Device Hash: ";
            SparseArray<byte[]> manufacturer_specific = scanDeviceList.get(i).getScanRecord().getManufacturerSpecificData();
            for(int j = 0; j < manufacturer_specific.size(); j++) {
                byte[] ba = manufacturer_specific.valueAt(j);
                if (ba != null) {
                    String hexKey = Integer.toHexString(manufacturer_specific.keyAt(j)).toUpperCase();
                    while (hexKey.length() < 4) {
                        hexKey = "0" + hexKey;
                    }
                    hexKey = "0x" + hexKey;
                    toAddHash += hexKey + " " + bytesToHex(ba);
                    toAddHash += "\n" + ba.length + " bytes";
                    if (BTCompanyIdentifierHash.containsKey(hexKey)) {
                        if (BTCompanyIdentifierHash.get(hexKey).equals("Microsoft")) {
                            String windowsDeviceType = "";
                            String deviceTypeFromHex = bytesToHex(ba).substring(2,4);
                            switch (deviceTypeFromHex) {
                                case "01":
                                    windowsDeviceType = ", Xbox One";
                                    break;
                                case "06":
                                    windowsDeviceType = ", Apple iPhone";
                                    break;
                                case "07":
                                    windowsDeviceType = ", Apple iPad";
                                    break;
                                case "08":
                                    windowsDeviceType = ", Android device";
                                    break;
                                case "09":
                                    windowsDeviceType = ", Windows 10 Desktop";
                                    break;
                                case "11":
                                    windowsDeviceType = ", Windows 10 Phone";
                                    break;
                                case "12":
                                    windowsDeviceType = ", Linux device";
                                    break;
                                case "13":
                                    windowsDeviceType = ", Windows IoT";
                                    break;
                                case "14":
                                    windowsDeviceType = ", Surface Hub";
                                    break;
                            }
                            viewHolder.deviceManufacturer.setText("Manufacturer: " + BTCompanyIdentifierHash.get(hexKey) + windowsDeviceType);
                        }
                        else {
                            viewHolder.deviceManufacturer.setText("Beacon Manufacturer: " + BTCompanyIdentifierHash.get(hexKey));
                        }
                    } else {
                        viewHolder.deviceManufacturer.setText("Beacon Manufacturer: " + hexKey);
                    }
                }
            }
            if (toAddHash != "Device Hash: ") {
                viewHolder.deviceHashCode.setText(toAddHash);
            }

            String toAddRSSI = "RSSI: " + scanDeviceList.get(i).getRssi();
            viewHolder.deviceRSSI.setText(toAddRSSI);

            String toAddCount = "Number of advertisement packets obtained: " + scanDeviceList.get(i).getCount();
            viewHolder.devicePacketsReceived.setText(toAddCount);

            ArrayList<Long> tsnList = scanDeviceList.get(i).getTimestampNanos();

            Long calculated = calcTimestampNanos(tsnList);

            long rxTimestampMillis = System.currentTimeMillis() -
                    SystemClock.elapsedRealtime() +
                    calculated / 1000000;

            initialTime = scanDeviceList.get(i).getInitialTime();
            long trueInitialTime = System.currentTimeMillis() -
                    SystemClock.elapsedRealtime() +
                    initialTime.getTime() / 1000000;
            if ((rxTimestampMillis - trueInitialTime) == 0L
                    || scanDeviceList.get(i).getCalculatedInterval() == 0L
            || scanDeviceList.get(i).getCount() == 0) {
                viewHolder.deviceAdvertisingInterval.setText("Advertising Interval:");
                viewHolder.deviceAdvertisingInterval.setVisibility(View.GONE);
            }
            else {
                Date rxDate = new Date(scanDeviceList.get(i).getCalculatedInterval() / 1000000);
                String sDate = new SimpleDateFormat("ssSSS").format(rxDate);
                while (sDate.substring(0, 1).equals("0")) {
                    sDate = sDate.substring(1);
                }

                String toAddAdvInt = "Advertising Interval: " + sDate + " ms";

                viewHolder.deviceAdvertisingInterval.setText(toAddAdvInt);
                viewHolder.deviceAdvertisingInterval.setVisibility(View.VISIBLE);
            }


            return view;
        }

    }

    // Device scan callback.
    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ScanDevice scannedobj = new ScanDevice();
                            scannedobj.setDevice(result.getDevice());
                            scannedobj.setScanRecord(result.getScanRecord());
                            scannedobj.setUuidList(getServiceUUIDsList(result));
                            scannedobj.setRssi(result.getRssi());
                            scannedobj.setTimestampNanos(result.getTimestampNanos());
                            scannedobj.setCount(1);
                            scannedobj.setInitialTime(new Timestamp(result.getTimestampNanos()));
                            scannedobj.setLegacy(result.isLegacy());
                            scannedobj.setAdvFlags(result.getScanRecord().getAdvertiseFlags());
                            mLeDeviceListAdapter.addScanDevice(scannedobj);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private List<UUID> getServiceUUIDsList(ScanResult scanResult)
    {
        List<ParcelUuid> parcelUuids = scanResult.getScanRecord().getServiceUuids();
        List<UUID> serviceList = new ArrayList<>();

        if (parcelUuids != null) {
            for (int i = 0; i < parcelUuids.size(); i++) {
                UUID serviceUUID = parcelUuids.get(i).getUuid();
                if (!serviceList.contains(serviceUUID))
                    serviceList.add(serviceUUID);
            }
        }
        return serviceList;
    }

    static class ViewHolder {
        LinearLayout listitemDevicesLayout;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceBluetoothClass;
        TextView deviceLegacy;
        TextView deviceAdvFlags;
        TextView deviceBondState;
        TextView deviceType;
        TextView deviceManufacturer;
        TextView deviceRSSI;
        TextView deviceHashCode;
        TextView devicePacketsReceived;
        TextView deviceAdvertisingInterval;
    }
}