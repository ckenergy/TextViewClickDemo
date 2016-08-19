package cn.cc.net.textviewclickdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView textView;

    String str = "@Weibo 来看看别人家的material design设计|自从去年google在android新版本Lollipop提出的material design概念后，" +
            "在世界各地疯狂地掀起一阵#material design#";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.txt);
        textView.setText(WeiBoContentTextUtil.getWeiBoContent(str,this,textView));
    }
}
