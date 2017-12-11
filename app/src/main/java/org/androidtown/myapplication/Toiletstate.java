package org.androidtown.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 박재성 on 2017-12-09.
 */

public class Toiletstate extends AppCompatActivity {

    //UI
    private TextView mConnectionStatus, toileCount;
    private ImageView toilet1, toilet2;
    private Button reservation1, reservation2;
    private Button refresh;

    //TAG
    private static final String TAG = "TcpClient";

    //ip 주소와 port번호
    private String mServerIP = "192.168.35.226";


    private int mPort = 8090;

    //그외 연결 변수들
    private boolean isConnected = false;
    private Socket mSocket;

    private PrintWriter mOut;
    private BufferedReader mIn;
    private Thread mReceiverThread = null;

    //화장실 사용가능 여부
    private int t1 = 0;
    private int t2 = 0;

    //화장실 사용가능 수
    private int total = t1 + t2;

    //UI변경을 위한 핸들러
    public static Handler handler1, handler2, handler3;

    //초기화 작업
    public Toiletstate() {
        mSocket = null;
    }

    //타이머
    private TextView toilet1UseTime;
    private TextView toilet2UseTime;

    private  int value = 0;
    private  int value2 = 0;

    private  boolean ischecked1 = false;
    private  boolean ischecked2 = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toilet_state);

        //연결되었다는 것을 나타내는 UI
        mConnectionStatus = (TextView) findViewById(R.id.connection_status_textview);

        //화장실 수
        toileCount = (TextView) findViewById(R.id.toileCount);

        //화장실 그림
        toilet1 = (ImageView) findViewById(R.id.toilet1);
        toilet2 = (ImageView) findViewById(R.id.toilet2);

        //화장실 사용여부 확인 가능한 버튼들
        reservation1 = (Button) findViewById(R.id.reservation1);
        reservation2 = (Button) findViewById(R.id.reservation2);

        //새로고침 버튼
        refresh = (Button) findViewById(R.id.refresh);

        //사용시간
        toilet1UseTime = (TextView) findViewById(R.id.toilet1UseTime);
        toilet2UseTime = (TextView) findViewById(R.id.toilet2UseTime);

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendMessage = "refresh";
                connectCheck(sendMessage);
            }
        });

        reservation1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendMessage = "1";
                connectCheck(sendMessage);

                try {
                    Thread.sleep(500);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                refresh.callOnClick();
            }
        });

        reservation2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sendMessage = "2";
                connectCheck(sendMessage);

                try {
                    Thread.sleep(500);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                refresh.callOnClick();

            }
        });

        //아이피 입력
        new Thread(new Toiletstate.ConnectThread(mServerIP, mPort)).start();

        //total count
        handler1 = new Handler() {
            public void handleMessage(Message msg) {
                total = (int) msg.obj;
                toileCount.setText(total + "");
            }
        };

        //toilet1 image change
        handler2 = new Handler() {
            public void handleMessage(Message msg) {
                int toiletState = (int) msg.obj;

                if (toiletState == 10) {
                    toilet1.setBackgroundResource(R.drawable.impossible);
                    reservation1.setEnabled(false);
                } else if (toiletState == 11) {
                    toilet1.setBackgroundResource(R.drawable.possible);
                    reservation1.setEnabled(true);
                }
            }
        };

        //toilet2 image change
        handler3 = new Handler() {
            public void handleMessage(Message msg) {
                int toiletState = (int) msg.obj;
                if (toiletState == 20) {
                    toilet2.setBackgroundResource(R.drawable.impossible);
                    reservation2.setEnabled(false);
                } else if (toiletState == 21) {
                    toilet2.setBackgroundResource(R.drawable.possible);
                    reservation2.setEnabled(true);
                }
            }
        };
    }

    //타이머를 처리하기 위해 핸들러 객체 생성
    private Handler thandler = new Handler() {
        public void handleMessage(Message msg) {
            if (ischecked1 == true) {
                value++;
                toilet1UseTime.setText("사용시간 : " + value/60 + "분" + value % 60 + "초");
                //1초간의 지연 시간을 두어 1초후에 자기자신이 호출 되도록 한다.
                thandler.sendEmptyMessageDelayed(0, 1000);
            }
            else
            {
                toilet1UseTime.setText("사용시간 : " + value/60 + "분" + value % 60 + "초");
            }
        }
    };

    private Handler thandler2 = new Handler() {
        public void handleMessage(Message msg) {
            if (ischecked2 == true) {
                value2++;
                toilet2UseTime.setText("사용시간 : " + value2/60 + "분" + value2 % 60 + "초");
                //1초간의 지연 시간을 두어 1초후에 자기자신이 호출 되도록 한다.
                thandler2.sendEmptyMessageDelayed(0, 1000);
            }
            else
                toilet2UseTime.setText("사용시간 : " + value2/60 + "분" + value2 % 60 + "초");
        }
    };

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
                        mConnectionStatus.setText("연결성공(" + serverIP + ")");

                        mReceiverThread = new Thread(new Toiletstate.ReceiverThread());
                        mReceiverThread.start();
                        refresh.callOnClick();
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
                    //Toast.makeText(getApplicationContext(), "보낸 명령어는 : " + msg, Toast.LENGTH_SHORT).show();
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
                                //Toast.makeText(getApplicationContext(), "받은 명령어는 : " + recvMessage, Toast.LENGTH_SHORT).show();
                                if (recvMessage.equals("t1on")) {
                                    t1 = 11;
                                    value = 0;
                                    ischecked1 = false;
                                } else if (recvMessage.equals("t1off")) {
                                    if (t1 == 11) {
                                        ischecked1 = true;
                                        thandler.sendEmptyMessage(0);
                                    }
                                    t1 = 10;
                                }

                                if (recvMessage.equals("t2on")) {
                                    t2 = 21;
                                    //초기화
                                    value2 = 0;
                                    ischecked2 = false;
                                } else if (recvMessage.equals("t2off")) {
                                    if (t2 == 21) {
                                        ischecked2 = true;
                                        thandler2.sendEmptyMessage(0);
                                    }
                                    t2 = 20;
                                }

                                total = t1 + t2 - 30;

                                Message message1 = Toiletstate.handler1.obtainMessage(1, total);
                                Message message2 = Toiletstate.handler2.obtainMessage(1, t1);
                                Message message3 = Toiletstate.handler2.obtainMessage(1, t2);

                                Toiletstate.handler1.sendMessage(message1);
                                Toiletstate.handler2.sendMessage(message2);
                                Toiletstate.handler3.sendMessage(message3);
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

    public void connectCheck(String sendMessage) {
        //연결이 되지 않으면
        if (!isConnected) {
            showErrorDialog("서버로 접속된후 다시 해보세요.");
        } else {
            //sendMessage에 따라서 명령을 다르게 한다. 메시지 Thread클래스로 전달
            switch (sendMessage) {
                case "refresh":
                    //Toast.makeText(getApplicationContext(), "전체 화장실의 상태를 물어봅니다", Toast.LENGTH_SHORT).show();
                    new Thread(new Toiletstate.SenderThread(sendMessage)).start();
                    break;
                case "1":
                    //Toast.makeText(getApplicationContext(), "1번 화장실을 예약합니다.", Toast.LENGTH_SHORT).show();
                    new Thread(new Toiletstate.SenderThread(sendMessage)).start();
                    break;
                case "2":
                    //Toast.makeText(getApplicationContext(), "2번 화장실을 예약합니다.", Toast.LENGTH_SHORT).show();
                    new Thread(new Toiletstate.SenderThread(sendMessage)).start();
                    break;
            }
        }
    }

    public void renew() {
        Intent intent = new Intent(getApplicationContext(), Toiletstate.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
