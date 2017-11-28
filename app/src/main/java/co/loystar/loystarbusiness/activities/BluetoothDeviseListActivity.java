package co.loystar.loystarbusiness.activities;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import co.loystar.loystarbusiness.R;

public class BluetoothDeviseListActivity extends ListActivity {
    private static final String TAG = BluetoothDeviseListActivity.class.getSimpleName();
    public static final int REQUEST_CONNECT_BT = 0x2300;
    private static final int REQUEST_ENABLE_BT = 0x1000;
    private static BluetoothAdapter mBluetoothAdapter = null;
    private static ArrayAdapter<String> mArrayAdapter = null;
    private static ArrayAdapter<BluetoothDevice> btDevices = null;
    private static BluetoothSocket mbtSocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_bluetooth_devise_list);

        setTitle("Bluetooth Devices");
        try {
            if (initDevicesList() != 0) {
                this.finish();
                return;
            }

        } catch (Exception ex) {
            this.finish();
            return;
        }

        IntentFilter btIntentFilter = new IntentFilter(
                BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBTReceiver, btIntentFilter);
    }

    public static BluetoothSocket getSocket() {
        return mbtSocket;
    }

    private void flushData() {
        try {
            if (mbtSocket != null) {
                mbtSocket.close();
                mbtSocket = null;
            }

            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.cancelDiscovery();
            }

            if (btDevices != null) {
                btDevices.clear();
                btDevices = null;
            }

            if (mArrayAdapter != null) {
                mArrayAdapter.clear();
                mArrayAdapter.notifyDataSetChanged();
                mArrayAdapter.notifyDataSetInvalidated();
                mArrayAdapter = null;
            }
            finalize();

        } catch (Exception ignored) {
        } catch (Throwable ignored) {
        }

    }

    private int initDevicesList() {
        flushData();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),
                    "Bluetooth not supported!!", Toast.LENGTH_LONG).show();
            return -1;
        }

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        mArrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_list_item_1);

        setListAdapter(mArrayAdapter);

        Intent enableBtIntent = new Intent(
                BluetoothAdapter.ACTION_REQUEST_ENABLE);
        try {
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } catch (Exception ex) {
            return -2;
        }

        Toast.makeText(getApplicationContext(),
                "Getting all available Bluetooth Devices", Toast.LENGTH_SHORT)
                .show();

        return 0;
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent intent) {
        super.onActivityResult(reqCode, resultCode, intent);

        switch (reqCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    Set<BluetoothDevice> btDeviceList = mBluetoothAdapter
                            .getBondedDevices();
                    try {
                        if (btDeviceList.size() > 0) {

                            for (BluetoothDevice device : btDeviceList) {
                                if (!btDeviceList.contains(device)) {

                                    btDevices.add(device);

                                    mArrayAdapter.add(device.getName() + "\n"
                                            + device.getAddress());
                                    mArrayAdapter.notifyDataSetInvalidated();
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                break;
        }
        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                try {
                    if (btDevices == null) {
                        btDevices = new ArrayAdapter<>(
                                getApplicationContext(), android.R.layout.simple_list_item_activated_1);
                    }

                    if (btDevices.getPosition(device) < 0) {
                        btDevices.add(device);
                        mArrayAdapter.add(device.getName() + "\n"
                                + device.getAddress() + "\n" );
                        mArrayAdapter.notifyDataSetInvalidated();
                    }
                } catch (Exception ignored) {
                }
            }
        }
    };

    @Override
    protected void onListItemClick(ListView l, View v, final int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (mBluetoothAdapter == null) {
            return;
        }

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        BluetoothDevice bluetoothDevice = btDevices.getItem(position);
        if (bluetoothDevice != null) {
            String txt = "Connecting to " + bluetoothDevice.getName() + ", " + bluetoothDevice.getAddress();
            Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_LONG).show();

            Thread connectThread = new Thread(() -> {
                try {
                    boolean uuidsWithSdp = bluetoothDevice.fetchUuidsWithSdp();
                    ParcelUuid[] parcelUuid = bluetoothDevice.getUuids();
                    if (parcelUuid != null) {
                        Log.e(TAG, "parcelUuidExists: " + uuidsWithSdp );
                        UUID uuid = parcelUuid[0].getUuid();
                        mbtSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                        mbtSocket.connect();
                    }
                    Log.e(TAG, "NoparcelUuid: " + uuidsWithSdp );
                } catch (IOException ex) {
                    runOnUiThread(socketErrorRunnable);
                    try {
                        mbtSocket.close();
                    } catch (IOException ignored) {
                    }
                    mbtSocket = null;
                } finally {
                    runOnUiThread(this::finish);
                }
            });

            connectThread.start();
        }
    }

    private Runnable socketErrorRunnable = () -> {
        Toast.makeText(getApplicationContext(),
                "Cannot establish connection", Toast.LENGTH_SHORT).show();
        mBluetoothAdapter.startDiscovery();
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, Menu.FIRST, Menu.NONE, "Refresh Scanning");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case Menu.FIRST:
                initDevicesList();
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mBTReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
