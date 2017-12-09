package org.androidtown.myapplication;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by 박재성 on 2017-12-09.
 */

public class Toiletstate extends AppCompatActivity {

    //UI
    private TextView mConnectionStatus;
    private Button toilet1, toilet2, toilet3;

    //TAG
    private static final String TAG = "TcpClient";

    //연결 변수들
    private boolean isConnected = false;
    private String mServerIP = null;
    private Socket mSocket;
    private PrintWriter mOut;
    private BufferedReader mIn;
    private Thread mReceiverThread = null;

    public Toiletstate() {
        mSocket = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toilet_state);

        //연결되었다는 것을 나타내는 UI
        mConnectionStatus = (TextView) findViewById(R.id.connection_status_textview);

        //화장실 사용여부 확인 가능한 버튼들
        toilet1 = (Button) findViewById(R.id.toilet1);
        toilet2 = (Button) findViewById(R.id.toilet2);
        toilet3 = (Button) findViewById(R.id.toilet3);

        toilet1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendMessage = "1";

                //연결이 되지 않으면
                if (!isConnected) {
                    showErrorDialog("서버로 접속된후 다시 해보세요.");
                } else {
                    //보내고 나서 EDIT 박스를 초기화 시켜준다.
                    Toast.makeText(getApplicationContext(),"1번 화장실의 상태를 물어봅니다",Toast.LENGTH_SHORT).show();
                    new Thread(new Toiletstate.SenderThread(sendMessage)).start();
                }

            }
        });

        new Thread(new Toiletstate.ConnectThread("192.168.35.221", 8090)).start();
    }


    private class ConnectThread implements Runnable {

        private String serverIP;
        private int serverPort;

        ConnectThread(String ip, int port) {
            serverIP = ip;
            serverPort = port;

            mConnectionStatus.setText("연결중(" + serverIP + ")");
        }

        @Override
        public void run() {

            try {
                mSocket = new Socket(serverIP, serverPort);
                //ReceiverThread: java.net.SocketTimeoutException: Read timed out 때문에 주석처리
                //mSocket.setSoTimeout(3000);
                mServerIP = mSocket.getRemoteSocketAddress().toString();

            } catch (UnknownHostException e) {
                Log.d(TAG, "ConnectThread: can't find host");
            } catch (SocketTimeoutException e) {
                Log.d(TAG, "ConnectThread: timeout");
            } catch (Exception e) {
                Log.e(TAG, ("ConnectThread:" + e.getMessage()));
            }


            if (mSocket != null) {
                try {
                    mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream(), "UTF-8")), true);
                    mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream(), "UTF-8"));

                    //연결이 되었을 경우 TRUE로 바꿔준다.
                    isConnected = true;
                } catch (IOException e) {
                    Log.e(TAG, ("ConnectThread:" + e.getMessage()));
                }
            }

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    if (isConnected) {
                        Log.d(TAG, "connected to " + serverIP);
                        mConnectionStatus.setText("연결성공(" + serverIP +")");

                        mReceiverThread = new Thread(new Toiletstate.ReceiverThread());
                        mReceiverThread.start();
                    } else {
                        Log.d(TAG, "failed to connect to server " + serverIP);
                        mConnectionStatus.setText("연결실패(" + serverIP + ")");
                    }
                }
            });
        }
    }

    //보낸 메시지 처리
    private class SenderThread implements Runnable {

        private String msg;

        SenderThread(String msg) {
            this.msg = msg;
        }

        @Override
        public void run() {

            mOut.println(this.msg);
            mOut.flush();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "send message: " + msg);
                    Toast.makeText(getApplicationContext(),"보낸 명령어는 : " + msg,Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //받는 메시지 처리
    private class ReceiverThread implements Runnable {

        @Override
        public void run() {

            try {
                while (isConnected) {

                    if (mIn == null) {
                        Log.d(TAG, "ReceiverThread: mIn is null");
                        break;
                    }

                    final String recvMessage = mIn.readLine();

                    if (recvMessage != null) {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Log.d(TAG, "recv message: " + recvMessage);
                                Toast.makeText(getApplicationContext(),"받은 명령어는 : " + recvMessage,Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                Log.d(TAG, "ReceiverThread: thread has exited");
                if (mOut != null) {
                    mOut.flush();
                    mOut.close();
                }

                mIn = null;
                mOut = null;

                if (mSocket != null) {
                    try {
                        mSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "ReceiverThread: " + e);
            }
        }
    }

    //엑티비티가 모두 종료 되었을경우
    @Override
    protected void onDestroy() {
        super.onDestroy();

        isConnected = false;
    }


    //에러 났을시 메시지를 띄어주고, 화면을 꺼준다.
    public void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }

}
