package org.zky.timepartruler;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TimePartRuler tv_main1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_main1 = (TimePartRuler) findViewById(R.id.tv_main1);

        List<TimePartRuler.TimePart> t=new ArrayList<>();
        t.add(new TimePartRuler.TimePart(7,0,0,7,30,0,"#dddddd","休息",true));
        t.add(new TimePartRuler.TimePart(7,30,0,8,0,0,"#7195bc","已锁定",true));
        t.add(new TimePartRuler.TimePart(8,0,0,8,30,0,"#f6b032","已预约",false));
        t.add(new TimePartRuler.TimePart(8,30,0,11,0,0,"#cfe7bd","",false));
        t.add(new TimePartRuler.TimePart(8,30,0,9,30,0,"#ff0000","推荐时间",false,true,this));
        tv_main1.addTimePart(t);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tv_main1.scrollTimeTo(7);
            }
        },500);
    }

    public void scroll(View view) {
        tv_main1.scrollTimeTo(8);
    }
}
