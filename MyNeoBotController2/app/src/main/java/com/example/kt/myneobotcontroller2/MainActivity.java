package com.example.kt.myneobotcontroller2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements View.OnTouchListener , View.OnClickListener {

    // Debugging
    private static final String TAG = "MyNeoBotBluetooth";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // NEO Direction
    public static final int DIRECTION_RIGHT          = 1;
    public static final int DIRECTION_GO_RIGHT       = 2;
    public static final int DIRECTION_GO            = 3;
    public static final int DIRECTION_GO_LEFT      = 4;
    public static final int DIRECTION_LEFT         = 5;
    public static final int DIRECTION_REVERSE_LEFT = 6;
    public static final int DIRECTION_REVERSE       = 7;
    public static final int DIRECTION_REVERSE_RIGHT  = 8;

    // NEO Slow Direction
    public static final int DIRECTION_SLOW_GO          = 1;
    public static final int DIRECTION_SLOW_RIGHT       = 2;
    public static final int DIRECTION_SLOW_REVERSE	= 3;
    public static final int DIRECTION_SLOW_LEFT      = 4;

    byte[] go = {0x0B};
    byte[] goRight = {0x14};
    byte[] goLeft = {0x15};
    byte[] right = {0x0D};
    byte[] left = {0x0E};
    byte[] reverseRight = {0x16};
    byte[] reverseLeft = {0x17};
    byte[] reverse = {0x0C};
    byte[] stop = {0x0F};

    byte[] slowGo = {0x09};
    byte[] slowRight = {0x08};
    byte[] slowLeft = {0x07};
    byte[] slowReverse = {0x0A};

    byte[] number1 = {0x01};
    byte[] number2 = {0x02};
    byte[] number3 = {0x03};
    byte[] number4 = {0x04};

    static private CommandThread mCommandThread;

    ImageView controllBall = null;          // controller ball 네오봇 제어 볼
    ImageView number1Btn = null;
    ImageView number2Btn = null;
    ImageView number3Btn = null;
    ImageView number4Btn = null;
    ImageView pairing = null;


    FrameLayout controllPad = null;
    LinearLayout numberPad = null;

    ImageView dot1 = null;
    ImageView dot2 = null;
    ImageView dot3 = null;
    ImageView dot4 = null;
    ImageView dot5 = null;
    ImageView dot6 = null;
    ImageView dot7 = null;
    ImageView dot8 = null;

    ImageView searching = null;


    boolean firsttouch = false;

    float controllBall_X ;
    float controllBall_y;

    int controllPadCentX;           // 컨트롤 패드의 중심점X
    int controllPadCentY;           // 컨트롤 패드의 중심점Y
    int controllWidth;              // 컨트롤 패드의 폭
    int controllHeight;             // 컨트롤 패드의 높이

    double[] pan;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private MyNeoBotBluetoothService mNeoBotBluetoothService = null;


    public class CommandThread extends Thread {
        private boolean commandCheck;
        private byte[] commandString;

        public CommandThread() {
            commandCheck = false;
            commandString = null;
        }


        public void run() {

            while (true) {
                try {
                    if (commandCheck){
                        Thread.sleep(30); // TODO - 통신 요구사항 - 누르고 있는 상태에서 값을 30ms로 계속 전송(40)

                        if (mNeoBotBluetoothService.getState() != MyNeoBotBluetoothService.STATE_CONNECTED) {
                            continue;
                        }
                        if (commandString != null) {
                            // TODO - 통신 요구사항 - 숫자버튼(2,3,4,5)을 누르면 해당 코드 값을 제어기에 전송

                            mNeoBotBluetoothService.write(commandString);
                        }
                    }

                } catch (Exception e) {

                Log.e("CommandThread", "catch Excpetion", e);

            }
            }
        }

        public void setCommandcheck(boolean _commandCheck)
        {

                commandCheck=_commandCheck;
        }
        public void setCommandString(byte []  _commandString)
        {
            commandString = _commandString;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setPan();
        controllBall = (ImageView) findViewById(R.id.controllBall);
        number1Btn = (ImageView) findViewById(R.id.btn_number1);
        number2Btn = (ImageView) findViewById(R.id.btn_number2);
        number3Btn = (ImageView) findViewById(R.id.btn_number3);
        number4Btn = (ImageView) findViewById(R.id.btn_number4);
        pairing = (ImageView) findViewById(R.id.pairing);

        controllPad = (FrameLayout) findViewById(R.id.controllpad);
        numberPad = (LinearLayout) findViewById(R.id.numberpad);

        controllPad.setOnTouchListener(this);
        number1Btn.setOnTouchListener(this);
        number2Btn.setOnTouchListener(this);
        number3Btn.setOnTouchListener(this);
        number4Btn.setOnTouchListener(this);

        dot1 = (ImageView) findViewById(R.id.dot1);
        dot2 = (ImageView) findViewById(R.id.dot2);
        dot3 = (ImageView) findViewById(R.id.dot3);
        dot4 = (ImageView) findViewById(R.id.dot4);
        dot5 = (ImageView) findViewById(R.id.dot5);
        dot6 = (ImageView) findViewById(R.id.dot6);
        dot7 = (ImageView) findViewById(R.id.dot7);
        dot8 = (ImageView) findViewById(R.id.dot8);

        searching = (ImageView) findViewById(R.id.serching);
        searching.setOnClickListener(this);
        pairing.setOnClickListener(this);

        mCommandThread = new CommandThread();
        mCommandThread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mNeoBotBluetoothService == null) {
                // Initialize the BluetoothChatService to perform bluetooth connections
                mNeoBotBluetoothService = new MyNeoBotBluetoothService(this, mHandler);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mNeoBotBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mNeoBotBluetoothService.getState() == MyNeoBotBluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mNeoBotBluetoothService.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mNeoBotBluetoothService != null) mNeoBotBluetoothService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    // 화면이 처음 뜰때 , 화면 건드릴 때
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(!firsttouch) {
            controllPadCentX = (controllPad.getWidth())/2;           // 중심점
            controllPadCentY = controllPad.getHeight()/2;
            controllWidth = controllBall.getWidth()/2;
            controllHeight = controllBall.getHeight()/2;
            controllBall.setX(controllBall_X = controllPadCentX - controllWidth);
            controllBall.setY(controllBall_y = controllPadCentY - controllHeight);
            firsttouch= true;
//            Toast.makeText(getApplication(), Integer.toString(controllPadCentX) + "," + Integer.toString(controllPadCentY), Toast.LENGTH_SHORT).show();
        }
    }
/*

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.insecure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }
*/

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
/*
//    *//**
//     * Sends a message.
//     * @param message  A string of text to send.
//     *//*
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mNeoBotBluetoothService.getState() != MyNeoBotBluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            byte[] send2 = {0x0B};

            mNeoBotBluetoothService.write(send2);

        }
    }*/

    private final void setStatus(int resId) {
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case MyNeoBotBluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
//                            mConversationArrayAdapter.clear();
                            break;
                        case MyNeoBotBluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case MyNeoBotBluetoothService.STATE_LISTEN:
                        case MyNeoBotBluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);

                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    // TODO - GUI 요구사항 - 네오봇블루투스에 연결되면 링크표시(7)가 밝은 파란색으로 표시
                    pairing.setImageResource(R.drawable.pairing_on);//////////////////////////////////////////////////////////////////////////

                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
//            case REQUEST_CONNECT_DEVICE_INSECURE:
//                // When DeviceListActivity returns with a device to connect
//                if (resultCode == Activity.RESULT_OK) {
//                    connectDevice(data, false);
//                }
//                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a session
                    // Initialize the BluetoothChatService to perform bluetooth connections
                    mNeoBotBluetoothService = new MyNeoBotBluetoothService(this, mHandler);
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mNeoBotBluetoothService.connect(device, secure);
    }


    double getDistance(float x, float y){

        double distance = Math.sqrt(Math.pow(controllPadCentX - x, 2) + Math.pow(controllPadCentY - y, 2));
        return distance;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.serching :
                Intent serverIntent = null;
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                break;

            case R.id.pairing :
                // TODO - 통신 요구사항 - 링크표시(7)를 누르면 블루투스 연결해제
                if(mNeoBotBluetoothService.getState()==MyNeoBotBluetoothService.STATE_CONNECTED) {
                    mNeoBotBluetoothService.stop();
                    pairing.setImageResource(R.drawable.pairing_off);
                }
                break;
        }
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        if(view.getId() == R.id.controllpad){
            onTouchControllPad(motionEvent);
        }
        else {
            onTouchButton(view, motionEvent);
        }

        return true;
    }

    private  void onTouchControllPad(MotionEvent event){

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE :  //TODO - GUI 요구사항 -  방향버튼(1)을 움직이면 터치된 위치로 방향버튼 이동
                float x = event.getX();
                float y = event.getY();

                double distance = getDistance(x, y);

                if(distance < 30) {
                    controllPad.setPressed(false);
                    controllBall.setX(x - controllWidth);
                    controllBall.setY(y - controllHeight);
                    break;
                }
                if(distance>160) {
                    return;
                }
                if (distance<100) // 저속
                {
                    goNeoSlow(x,y);

                } else goNeo(x, y); // 고속

                controllBall.setX(x - controllWidth);
                controllBall.setY(y - controllHeight);
                break;

            case MotionEvent.ACTION_UP:  // TODO - GUI 요구사항 -  방향버튼(1)에서 손을 때면 가운데로 위치이동
                controllBall.setX(controllBall_X);
                controllBall.setY(controllBall_y);
                controllPad.setPressed(false);

                mCommandThread.setCommandcheck(true);
                mCommandThread.setCommandString(stop);
                mCommandThread.setCommandcheck(false);

                break;
        }

    }

    private void onTouchButton(View view , MotionEvent motionEvent){

        // TODO - GUI 요구사항 -  숫자버튼(2,3,4,5)을 누르면 눌린상태 표시 -> drawable/select_btn_num1~4 에서 구현
        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                view.setPressed(true);
                mCommandThread.setCommandcheck(true);

                switch (view.getId()){
                    case R.id.btn_number1:
                        mCommandThread.setCommandString(number1);
                        break;
                    case R.id.btn_number2:
                        mCommandThread.setCommandString(number2);
                        break;
                    case R.id.btn_number3:
                        mCommandThread.setCommandString(number3);
                        break;
                    case R.id.btn_number4:
                        mCommandThread.setCommandString(number4);
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                view.setPressed((false));
                view.performClick();

                mCommandThread.setCommandcheck(true);
                mCommandThread.setCommandString(stop);
                mCommandThread.setCommandcheck(false);
//                this.commandCheck=true;
//                this.commandString = stop;
                //this.commandCheck=false;

                break;
        }

    }

    // tan 30, tan 60 값 저장
    void setPan() {
        pan = new double[3];

        pan[0] = Math.tan(Math.toRadians(30));
        pan[1] = Math.tan(Math.toRadians(60));
        pan[2] = Math.tan(Math.toRadians(45));
    }

    int getPoint(float _x, float _y) {

        /**
         * 우회전 1
         * 전진우회전 2
         * 전진 3
         * 전진좌회전 4
         * 좌회전 5
         * 후진좌회전 6
         * 후진 7
         * 후진우회전 8
         */



        float x = _x - controllPadCentX;
        float y = controllPadCentY - _y;

        // 경계
        if (y == 0) {
            if (x < 0)
                return DIRECTION_LEFT;
            else if (x > 0)
                return DIRECTION_RIGHT;
            return 0;
        }

        if (x == 0) {
            if (y > 0)
                return DIRECTION_GO;
            else if (y < 0)
                return DIRECTION_REVERSE;

            return 0;
        }
        // 1사분면
        if ((x > 0) && (y > 0)) {
            double tan_xy = (double) (Math.abs(y) / Math.abs(x));

            if ((0 < tan_xy) && (tan_xy < pan[0]))
                return DIRECTION_RIGHT;
            else if ((pan[0] < tan_xy) && (tan_xy < pan[1]))
                return DIRECTION_GO_RIGHT;
            else if ((pan[1] < tan_xy))
                return DIRECTION_GO;

            return 0;
        }
        // 2사분면
        else if ((x < 0) && (y > 0)) {
            double tan_xy = (double) (Math.abs(y) / Math.abs(x));

            if ((0 < tan_xy) && (tan_xy < pan[0]))
                return DIRECTION_LEFT;
            else if ((pan[0] < tan_xy) && (tan_xy < pan[1]))
                return DIRECTION_GO_LEFT;
            else if ((pan[1] < tan_xy))
                return DIRECTION_GO;

            return 0;
        }
        // 3사분면
        else if ((x < 0) && (y < 0)) {
            double tan_xy = (double) (Math.abs(y) / Math.abs(x));

            if ((0 < tan_xy) && (tan_xy < pan[0]))
                return DIRECTION_LEFT;
            else if ((pan[0] < tan_xy) && (tan_xy < pan[1]))
                return DIRECTION_REVERSE_LEFT;
            else if ((pan[1] < tan_xy))
                return DIRECTION_REVERSE;

            return 0;
        }
        // 4사분면
        else if ((x > 0) && (y < 0)) {
            double tan_xy = (double) (Math.abs(y) / Math.abs(x));

            if ((0 < tan_xy) && (tan_xy < pan[0]))
                return DIRECTION_RIGHT;
            else if ((pan[0] < tan_xy) && (tan_xy < pan[1]))
                return DIRECTION_REVERSE_RIGHT;
            else if ((pan[1] < tan_xy))
                return DIRECTION_REVERSE;

            return 0;
        }
        else {}

        return 0;
    }

    void goNeo(float x, float y){ // TODO - GUI 요구사항 - 방향버튼(1)의 위치에 따라 방향표시등을 표시  dot1.setPressed(true), dot2.setPressed(true), dot3.setPressed(true) 으로 구현
                                    // TODO - 통신 요구사항 - 방향버튼(1)을 움직이면 해당 위치의 코드 값을 전송

        int directionValue = getPoint(x, y);
        String direction =" ";

        switch(directionValue){
            case DIRECTION_RIGHT :
                direction = "right";
                controllPad.setPressed(false);
                dot3.setPressed(true);


                mCommandThread.setCommandString(right);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_GO_RIGHT :
                direction = "go_right";
                controllPad.setPressed(false);
                dot2.setPressed(true);
                mCommandThread.setCommandString(goRight);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_GO :
                direction = "go";
                controllPad.setPressed(false);
                dot1.setPressed(true);
                mCommandThread.setCommandString(go);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_GO_LEFT:
                direction = "go_left";
                controllPad.setPressed(false);
                dot8.setPressed(true);
                mCommandThread.setCommandString(goLeft);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_LEFT :
                direction = "left";
                controllPad.setPressed(false);
                dot7.setPressed(true);
                mCommandThread.setCommandString(left);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_REVERSE_LEFT:
                direction = "reverse_left";
                controllPad.setPressed(false);
                dot6.setPressed(true);
                mCommandThread.setCommandString(reverseLeft);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_REVERSE :
                direction = "reverse";
                controllPad.setPressed(false);
                dot5.setPressed(true);
                mCommandThread.setCommandString(reverse);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_REVERSE_RIGHT :
                direction = "reverse_right";
                controllPad.setPressed(false);
                dot4.setPressed(true);
                mCommandThread.setCommandString(reverseRight);
                mCommandThread.setCommandcheck(true);
                break;
        }

        Log.e("direction", direction);

    }

    int getPoint2(float _x, float _y){

        /**
         * 저속 전진   1
         * 저속 우회전 2
         * 저속 후진   3
         * 저속 좌회전 4
         **/
        float x = _x - controllPadCentX;
        float y = controllPadCentY - _y;

        // 경계
        if (y == 0) {
            if (x < 0)
                return DIRECTION_SLOW_LEFT;
            else if (x > 0)
                return DIRECTION_SLOW_RIGHT;

            return 0;
        }

        if (x == 0) {
            if (y > 0)
                return DIRECTION_SLOW_GO;
            else if (y < 0)
                return DIRECTION_SLOW_REVERSE;

            return 0;
        }
        // 1사분면
        if ((x > 0) && (y > 0)) {
            double tan_xy = (double) (Math.abs(y) / Math.abs(x));

            if ((0 < tan_xy) && (tan_xy < pan[2]))
                return DIRECTION_SLOW_RIGHT;
            else if ((pan[2] < tan_xy))
                return DIRECTION_SLOW_GO;

            return 0;
        }
        // 2사분면
        else if ((x < 0) && (y > 0)) {
            double tan_xy = (double) (Math.abs(y) / Math.abs(x));

            if ((0 < tan_xy) && (tan_xy < pan[2]))
                return DIRECTION_SLOW_LEFT;
            else if ((pan[2] < tan_xy))
                return DIRECTION_SLOW_GO;

            return 0;
        }
        // 3사분면
        else if ((x < 0) && (y < 0)) {
            double tan_xy = (double) (Math.abs(y) / Math.abs(x));

            if ((0 < tan_xy) && (tan_xy < pan[2]))
                return DIRECTION_SLOW_LEFT;
            else if ((pan[2] < tan_xy))
                return DIRECTION_SLOW_REVERSE;

            return 0;
        }
        // 4사분면
        else if ((x > 0) && (y < 0)) {
            double tan_xy = (double) (Math.abs(y) / Math.abs(x));

            if ((0 < tan_xy) && (tan_xy < pan[2]))
                return DIRECTION_SLOW_RIGHT;
            else if ((pan[2] < tan_xy))
                return DIRECTION_SLOW_REVERSE;

            return 0;
        }
        else {
        }

        return 0;
    }

    void goNeoSlow(float x , float y){
        int directionValue = getPoint2(x,y);
        String direction = "slow_";

        switch(directionValue){
            case DIRECTION_SLOW_GO:
                direction += "go";
                controllPad.setPressed(false);
                dot1.setPressed(true);
                mCommandThread.setCommandString(slowGo);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_SLOW_RIGHT:
                direction += "right";
                controllPad.setPressed(false);
                dot3.setPressed(true);
                mCommandThread.setCommandString(slowRight);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_SLOW_REVERSE:
                direction += "reverse";
                controllPad.setPressed(false);
                dot5.setPressed(true);
                mCommandThread.setCommandString(slowReverse);
                mCommandThread.setCommandcheck(true);
                break;
            case DIRECTION_SLOW_LEFT:
                direction += "left";
                controllPad.setPressed(false);
                dot7.setPressed(true);
                mCommandThread.setCommandString(slowLeft);
                mCommandThread.setCommandcheck(true);
                break;

        }
        Log.e("direction", direction);

    }

}
