package org.androidtown.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by 박재성 on 2017-12-09.
 */

public class MainActivity2 extends AppCompatActivity {

    //UI 변수들
    ImageButton enterToiletStateBtn;

    //뒤로가기 버튼
    long bpTime = 0;
    Toast bpToast;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        enterToiletStateBtn = (ImageButton) findViewById(R.id.enterToiletStateBtn);

        //종료 버튼 문구
        bpToast = Toast.makeText(this, "뒤로가기를 한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT);

        //화장실 상태 보기 버튼을 클릭할 경우
        enterToiletStateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Toiletstate.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        //참고 : http://best421.tistory.com/71
        //이렇게 해도 cancel이 안되는 이유는 onBackPressed()에 들어올 때마다 toast가 새로 생성되기 때문에
        //결국 cancel하는 toast는 다른 값이 된다. -> 전역변수로 설정하자.
//        Toast toast = Toast.makeText(this, "뒤로가기를 한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT);
        if (bpTime == 0) {
            System.out.println("토스트1 : " + bpToast);
            ViewGroup group = (ViewGroup) bpToast.getView();
            TextView messageTextView = (TextView) group.getChildAt(0);
            messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            bpToast.show();
            bpTime = System.currentTimeMillis();
        } else {
            long sec = System.currentTimeMillis() - bpTime;

            if (sec > 2000) {
                System.out.println("토스트2 : " + bpToast);
                bpToast.show();
                bpTime = System.currentTimeMillis();
            } else {
                bpToast.cancel();
                super.onBackPressed();
                finish();
            }
        }
    }
}
