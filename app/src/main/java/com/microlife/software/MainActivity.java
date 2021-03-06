package com.microlife.software;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

//import BluetoothLeService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import static java.lang.String.format;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    Context         mContext;
    PopupWindow     mPopupWindow;
    ProgressDialog  progressDialog;
    Button      Btn_A0ack;
    Button      Btn_A1ack;
    TextView    mDataText;
    TextView    mConnect;
    ScrollView  mScroller;

    private Handler handler = new Handler();
    boolean mScanning;
    boolean mInitialFinished = false;
    private boolean OpenDialog = false;
    boolean SearchBLE = false;
    boolean BLUETOOTH_ENABLE = false;
    boolean BLUETOOTH_RECONNECT = false;
    int     versionCode;
    String  versionName;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    static final float  adResolution = (float) 0.02f;

    List<byte[]>    A0ReciveList = new LinkedList<>();
    private byte[] A0Tmp = new byte[14];
    private byte[] A1Tmp = new byte[8];


    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize())
            {
                BLUETOOTH_ENABLE = false;
                finish();
            }
            else
            {
                BLUETOOTH_ENABLE = true;
            }
            // Automatically connects to the device upon successful start-up initialization.
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            mBluetoothLeService = null;
            mInitialFinished = false;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver()
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            //BLE Status Changed
            final String action = intent.getAction();

            Log.d(TAG, "onReceive(), action: " + action);     //debug

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action))                //裝置連線成功
            {
                invalidateOptionsMenu();
                mConnect.setText("Connected");
                InsertMessage(mBluetoothLeService.mBluetoothGattAddress+" Connected");

                if(OpenDialog)
                {
                    OpenDialog=false;
                    progressDialog.dismiss();
                }
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action))        //裝置斷線
            {
                Utils.writeLogFile(A0ReciveList);
                invalidateOptionsMenu();
                mConnect.setText("Disconnected");
                InsertMessage(mBluetoothLeService.mBluetoothGattAddress+" Disonnected");
                //parserData(A0ReciveList);

                if(OpenDialog)
                {
                    OpenDialog=false;
                    progressDialog.dismiss();
                }

                //if (A0ReciveList.size()>0) //debug
                //{
                //    for (int i=0; i<A0ReciveList.size(); i++)
                //    {
                //        //Log.d("Srv Event", "A0[" + i + "]: " + A0ReciveList.get(i).toString());
                //        LogDebugShow("Srv Event[" + i + "]", A0ReciveList.get(i));
                //    }
                //}
            }
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                // Show all the supported services and characteristics on the user interface.
                invalidateOptionsMenu();
            }
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action))
            {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                //displayData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
            else if (BluetoothLeService.ACTION_GATT_DEVICE_DISCOVERED.equals(action))
            {
                DiscoverGattDevice( intent.getStringExtra(BluetoothLeService.ACTION_mBluetoothDeviceName),
                                    intent.getStringExtra(BluetoothLeService.ACTION_mBluetoothDeviceAddress));
            }
            else if (BluetoothLeService.ACTION_Enable.equals(action))
            {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                Log.d(TAG, "ACTION_Enable: " + intent.getStringExtra(BluetoothLeService.ACTION_Enable));
                // /progressDialog.setMessage("Connected");
                //Notify Enabled & Send Command to BLE Device
                //CommandTest();
            }
            //else if (BluetoothLeService.COUNTDOWN_BR.equals(action))
            //{
            //    mBluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_GATT_DISCONNECTED);
            //}
            else if (BluetoothLeService.ACTION_Connect_Fail.equals(action))
            {}
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this.getApplicationContext();

        Btn_A0ack = (Button) findViewById(R.id.btn_A0ack);
        Btn_A1ack = (Button) findViewById(R.id.btn_A1ack);
        mDataText = (TextView) findViewById(R.id.DataText);
        mScroller = (ScrollView) findViewById(R.id.Scroller);
        mConnect = (TextView) findViewById(R.id.textView);

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                scanLeDevice(true);
                showPopupWindowEvent();
            }
        });

        appVersion();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Please turn Bluetooth power", Toast.LENGTH_SHORT).show();
            finish();
        }

        //receiveTmp=null;
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        startService(gattServiceIntent);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null)
        {
            finish();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION))
            {
            }
            else
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings)
        if (item.getItemId() == R.id.action_settings)
        {
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSION_REQUEST_COARSE_LOCATION:
            {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //  Log.d(TAG, "coarse location permission granted");
                }
                else
                {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener()
                    {
                        @Override
                        public void onDismiss(DialogInterface dialog)
                        {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unbindService(mServiceConnection);

        mBluetoothLeService.close();
        mBluetoothLeService = null;
        if(handler!=null)   handler.removeCallbacks(updateTimer);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        A0ReciveList.clear();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled())
        {
            if (!mBluetoothAdapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED)
        {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static IntentFilter makeGattUpdateIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DEVICE_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_Enable);
        intentFilter.addAction(BluetoothLeService.ACTION_Connect_Fail);

        return intentFilter;
    }

    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            mScanning = true;
            mBluetoothLeService.ScanDevice(true);
        }
        else
        {
            mScanning = false;
            mBluetoothLeService.ScanDevice(false);
        }
    }

    private void DiscoverGattDevice(String DeviceName, String DeviceAddress)
    {
        String Address = DeviceAddress.replaceAll(":", "");

        if (DeviceName == null)
        {
            DeviceName = "Unknow Device";
        }

        if (!SearchBLE && (!BLUETOOTH_RECONNECT || !OpenDialog))
        {
            mLeDeviceListAdapter.addDevice(DeviceName, DeviceAddress);
            mLeDeviceListAdapter.notifyDataSetChanged();
        }
    }

    public void cmdA1ack(View v)    // for Button A1ack onClick()
    {
        CommandTest((byte) 0xA1);
    }

    public void cmdA0ack(View v)    // for Button A0ack onClick()
    {
        CommandTest((byte) 0xA0);
    }

    private void displayGattServices(List<BluetoothGattService> gattServices)
    {
        if (gattServices == null) return;
    }

    private void appVersion()
    {
        try
        {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = packageInfo.versionCode;
            versionName = packageInfo.versionName;
            Log.d(TAG, "appVersion(), Ver. " + versionName + "." + versionCode);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(toolbar.getTitle() + "\t\t\t v" + versionName + "." + Integer.toString(versionCode));
    }

    private void CommandTest(byte command)
    {
        byte[] testCommand = new byte[0];

        if(!mBluetoothLeService.mBluetoothGattConnected)
            return;

        switch ((byte)command)
        {
            case (byte) 0xA0:
                testCommand = new byte[]{0x4D, (byte) 0xFD, 0x00, 0x02, (byte) 0xA0, (byte) 0xCE};
                break;

            case (byte) 0xA1:
                testCommand = Utils.mlcTestCommand((byte)0xA1);
                //testCommand = new byte[]{  0x4D, (byte) 0xFD, 0x00, 0x08, (byte) 0xA1,
                //                            (byte) 0x18, (byte) 0x7A, (byte) 0x93,
                //                            (byte) 0x03, (byte) 0x77, (byte) 0xDD,
                //                             0x00};
                //testCommand[testCommand.length - 1] = (byte) Utils.countCS(testCommand);
                break;

            default:
                break;
        }
        mBluetoothLeService.writeCharacteristicCMD(testCommand);
        Log.d(TAG, "Cmd, Write Command to device.");

        StringBuilder sb= new StringBuilder(testCommand.length);
        for (byte indx: testCommand)
        {
            sb.append(format("%02X", indx));
        }
        Log.d(TAG, "Cmd, Write Command to NC150: " + sb.toString());
        InsertMessage("T:" + sb.toString() + "\r\n");
    }

    private void LogDebugShow(String info, byte[] data)
    {
        for (int i=0;i<data.length; i++)
        {
            Log.d(TAG, "data [" + i + "]= " + format("0x%02X",data[i]));
        }
    }

    private void displayData(String data)
    {
        if (data != null)
        {
            byte[] byteArray = hexStringToByteArray(data);
            Log.d(TAG, "displayData(), device: " + data);

            if (byteArray[0] == 'M')
            {
                InsertMessage("R:" + data);
                Log.d(TAG, "displayData(), bA[4]: " + format("%02X", byteArray[4]) + ": "
                        + mBluetoothLeService.mBluetoothGattConnected);

                switch (byteArray[4])
                {
                    case (byte) 0xA0:
                        //Log.d(TAG, "displayData(), A0 Command found.");
                        //if (!java.util.Arrays.equals(A0Tmp, byteArray))
                        //{
                        //    Log.d("dD()", "Add A1 receive data to List.");
                        //    A0ReciveList.add(byteArray);
                        //    A0Tmp = byteArray.clone();
                        //    //LogDebugShow("A0 new item", A0ReciveList.get(A0ReciveList.size()-1));
                        //}
                        //Log.d(TAG, "displayData(), A0 List Size: " + A0ReciveList.size());

                        //CommandTest((byte) 0xA0);
                        //mBluetoothLeService.cdt.start();
                        mBluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
                        Log.d(TAG, "displayData(), 0xA1 Ack to BLE service.");
                        break;

                    case (byte) 0xA1:
                        //Log.d(TAG, "displayData(), A1 Command found.");
                        //if (!java.util.Arrays.equals(A1Tmp, byteArray))
                        //{
                        //    Log.d(TAG, "displayData(), Add A1 receive data to List.");
                        //    A0ReciveList.add(byteArray);
                        //    A1Tmp = byteArray.clone();
                        //    //LogDebugShow("A1 new item", A0ReciveList.get(A0ReciveList.size()-1));

                            CommandTest((byte) 0xA1);
                            //mBluetoothLeService.cdt.start();
                            mBluetoothLeService.broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
                            Log.d(TAG, "displayData(), 0xA0 Ack to BLE service.");
                        //}
                        break;

                    default:
                        break;
                }
            }

            if(OpenDialog)
            {
                OpenDialog = false;
                progressDialog.cancel();
            }
        }
    }

    private void parserData(List<byte[]> dataList)
    {
        if (dataList.size()>0) //debug
        {
            //mDataText.setText("");  // clean Text View
            InsertMessage("");
            for (int i=0; i<dataList.size(); i++)
            {
                //Log.d("Srv Event", "A0[" + i + "]: " + A0ReciveList.get(i).toString());
                //LogDebugShow("Srv Event[" + i + "]", dataList.get(i));
                messageParser(dataList.get(i));
            }
        }
    }

    private void messageParser(byte[] dataInfo)
    {
        String  A0Message = "";
        String  A1Message = "";

        switch (dataInfo[4])
        {
            case (byte) 0xA0:
                if ((dataInfo[12] & 0x0080) == 0x0080)  //check Error
                {
                    A0Message = measureError((byte) (dataInfo[12] & 0x003F));
                }
                else
                {
                    String  ambient = getTemperature(dataInfo[5], dataInfo[6]);
                    String  workModeStr = workMode((byte) ((dataInfo[7] & 0x0080) >>> 7));
                    String  measure = getTemperature((byte) (dataInfo[7] & 0x007F), dataInfo[8]);
                    String  ncfrDate = measureTime((dataInfo[9]), dataInfo[10], dataInfo[11],
                                                    (byte) (dataInfo[12] & 0x003F));
                    String  feverState = ((dataInfo[12] & 0x0040) == 0x0040) ? "fever" : "no fever";

                    A0Message = "Amb=" + ambient + ", " + workModeStr + "= " +
                                measure + ",\r\n" + feverState +", " + ncfrDate;
                }
                break;

            case (byte) 0xA1:
                A1Message = workMode(dataInfo[5]) + ", " + batteryState(dataInfo[6]);
                break;

            default:
                break;
        }
        InsertMessage(A1Message + A0Message + "\r\n");
    }

    private String workMode(byte mode)
    {
        String[] tmpStr= new String[]{"Body mode", "Object mode", "Memory mode"};

        if ((mode & 0x0080) == 0x80)  mode = 0x01;
        else if (mode > tmpStr.length)
            return ("mode error, code: " + mode);

        return (tmpStr[mode & 0x00ff]);
    }

    private String batteryState(byte adValue)
    {
        float   tmp = (float) (adResolution * (int) (adValue & 0x00ff));

        return (String.format("%4.2fV", tmp));
    }

    private String getTemperature(byte dataH, byte dataL)
    {
        int     tmpValue=0;
        tmpValue |= (int) (dataH & 0x00ff);
        tmpValue <<= 8;
        tmpValue |= (int) (dataL & 0x00ff);
        float ftmp = ((float) tmpValue) / 100;

        return(String.format("%4.2f℃", ftmp));
    }

    private String measureTime(byte mDay, byte mHour, byte mMinute, byte mYear)
    {
        if (((mDay & 0x00FF) == 0xFF) || ((mHour & 0x00FF) ==0xFF) || ((mMinute & 0x00FF)==0xFF))
        {
            return("Date/Time some one is 0xFF");
        }

        byte tmpMonth = 0;
        byte tmpYear = (byte)(mYear & 0x003F);
        byte tmpHour = (byte)(mHour & 0x003F);
        byte tmpDay =  (byte)(mDay & 0x003F);

        tmpMonth |= ((mHour & 0x00C0) >>> 2);
        tmpMonth |= (byte) (mDay & 0x00C0);
        tmpMonth >>= 4;

        return(String.format("%04d/%02d/%02d, %02d:%02d",
                ((int)tmpYear + 2000), (int)tmpMonth, (int)tmpDay, (int)tmpHour, (int)mMinute));
    }

    private String measureError(byte info)
    {
        String[]  ErrorMessage = {"Amb H", "Amb L", "Body H", "Body L"};

        if (info > ErrorMessage.length)
            return ("Unknown, Error! Code: " +  Integer.toHexString(info & 0x003f));
        else
            return (ErrorMessage[(info & 0x003f)] + ", Error!");
    }



    @Override
    public void onStop()
    {
        super.onStop();
    }

    private void showPopupWindowEvent()
    {
        dismissPopupWindow();
        View    rootView = getLayoutInflater().inflate(R.layout.popup_window, null);
        Display display = getWindowManager().getDefaultDisplay();
        Point   size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        ListView popupList = (ListView) rootView.findViewById(R.id.popup_list);
        Button popupButton = (Button) rootView.findViewById(R.id.button11);
        popupButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                scanLeDevice(false);
                //mDataText.setText("");
                dismissPopupWindow();
            }
        });

        mLeDeviceListAdapter.clear();
        popupList.setAdapter(mLeDeviceListAdapter);
        popupList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dismissPopupWindow();
                mDataText.setText("");
                OpenDialog("BLE Status", "Try to connect with device...");
                scanLeDevice(false);
                mBluetoothLeService.connect(mLeDeviceListAdapter.getDevice(position));
            }
        });

        mPopupWindow = new PopupWindow(rootView, 2 * width / 3, ActionBar.LayoutParams.WRAP_CONTENT, false);
        mPopupWindow.setAnimationStyle(R.style.popup_window);
        mPopupWindow.setOutsideTouchable(false);
        mPopupWindow.setFocusable(false);
        mPopupWindow.update();
        mPopupWindow.showAtLocation(getLayoutInflater().inflate(R.layout.activity_main, null),
                                    Gravity.CENTER, 0, 0);
    }

    private void dismissPopupWindow()
    {
        if (mPopupWindow != null)
        {
            mPopupWindow.dismiss();
        }
    }

    public static byte[] hexStringToByteArray(String s)
    {
        int getLength = s.length() % 2;

        if(getLength!=0)
        {
            s="0"+s;
        }

        int len = s.length();

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
                                    Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private void OpenDialog(String Title, String Meaasge)
    {
        if (!OpenDialog)
        {
            OpenDialog = true;
            progressDialog = new ProgressDialog(this);
            progressDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel",
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                        //    updateRemoveNodeState(RemoveNodeAppEvent.USER_CANCEL);
                        }
                    });
            progressDialog.setCancelable(false);
            progressDialog.setTitle(Title);
            progressDialog.setMessage(Meaasge);
            progressDialog.show();
        }
        else
        {
            progressDialog.setTitle(Title);
            progressDialog.setMessage(Meaasge);
        }
    }

    private void InsertMessage(String Message)
    {
        //mDataText.setText(mDataText.getText()+Message);
        mDataText.setText(mDataText.getText()+Message+"\r\n");
        //mDataText.append(Message);
        mScroller.fullScroll(ScrollView.FOCUS_DOWN);
    }

    private Runnable updateTimer = new Runnable()
    {
        public void run()
        {
            handler.postDelayed(this, 1000);
        }
    };

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter
    {
        private ArrayList<String> mLeDevices;
        private ArrayList<String> mMacAddress;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter()
        {
            super();
            mLeDevices = new ArrayList<String>();
            mMacAddress = new ArrayList<String>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(String Devicename, String MacAddress)
        {
            if (!mMacAddress.contains(MacAddress))
            {
                mLeDevices.add(Devicename);
                mMacAddress.add(MacAddress);
            }
        }

        public String getDevice(int position)
        {
            return mMacAddress.get(position);
        }

        public void clear()
        {
            mLeDevices.clear();
            mMacAddress.clear();
        }

        @Override
        public int getCount()
        {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i)
        {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            ViewHolder holder;

            String device = mMacAddress.get(i);
            String deviceNmae = mLeDevices.get(i);

            if (view == null)
            {
                holder = new ViewHolder();
                view = getLayoutInflater().inflate(R.layout.lst_items, null);
                holder.title = (TextView) view.findViewById(R.id.title);
                holder.subTitle = (TextView) view.findViewById(R.id.subTitle);
                view.setTag(holder);
            }
            else
            {
                holder = (ViewHolder) view.getTag();
            }

            holder.title.setText(deviceNmae);
            holder.subTitle.setText(device);
            return view;
        }
    }

    static class ViewHolder
    {
        TextView title;
        TextView subTitle;
    }
}

