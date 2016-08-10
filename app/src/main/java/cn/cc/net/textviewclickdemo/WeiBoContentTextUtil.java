package cn.cc.net.textviewclickdemo;

import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wenmingvs on 16/4/16.
 */
public class WeiBoContentTextUtil {

   static class Section {
       int start;
       int end;
        Section(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
    private static final String TAG = "WeiBoContentTextUtil";

    private static final String AT = "@[\\w\\p{InCJKUnifiedIdeographs}-]{1,26}";// @人
    private static final String TOPIC = "#[\\p{Print}\\p{InCJKUnifiedIdeographs}&&[^#]]+#";// ##话题

    private static final String ALL = "(" + AT + ")" + "|" + "(" + TOPIC + ")" ;

    public static SpannableStringBuilder getWeiBoContent(final String source, final Context context, final TextView textView) {

        final ArrayList<Section> sections = new ArrayList<>();
        final SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(source);
        //设置正则
        Pattern pattern = Pattern.compile(ALL);
        Matcher matcher = pattern.matcher(spannableStringBuilder);

        if (matcher.find()) {
            if (!(textView instanceof EditText)) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setFocusable(false);
                textView.setClickable(false);
                textView.setLongClickable(false);
            }
            matcher.reset();
        }

        while (matcher.find()) {
            final String at = matcher.group(1);
            final String topic = matcher.group(2);

            //处理@用户
            if (at != null) {
                int start = matcher.start(1);
                int end = start + at.length();
                Section section = new Section(start, end);
                sections.add(section);
                //再构造一个改变字体颜色的Span
                ForegroundColorSpan foregroundspan = new ForegroundColorSpan(Color.BLUE);
                spannableStringBuilder.setSpan(foregroundspan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            //处理##话题
            if (topic != null) {
                int start = matcher.start(2);
                int end = start + topic.length();
                Section section = new Section(start, end);
                sections.add(section);
                ForegroundColorSpan foregroundspan = new ForegroundColorSpan(Color.BLUE);
                spannableStringBuilder.setSpan(foregroundspan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }

        final int slop = ViewConfiguration.get(context).getScaledTouchSlop();
        final BackgroundColorSpan span = new BackgroundColorSpan(Color.YELLOW);

        textView.setOnTouchListener(new View.OnTouchListener() {

            int downX= -1,downY=-1;
            Section downSection = null;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                Layout layout = textView.getLayout();
                int line = 0;
                int index = 0;
                switch(action) {
                    case MotionEvent.ACTION_DOWN:
                        line = layout.getLineForVertical(textView.getScrollY()+ (int)event.getY());
                        index = layout.getOffsetForHorizontal(line, (int)event.getX());
                        Log.d(TAG," index:"+ index+",sections:"+sections.size());
                        for (Section section : sections) {
                            if ( index>=section.start &&  index <= section.end) {
                                spannableStringBuilder.setSpan(span,section.start,section.end,Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                                downSection = section;
                                textView.setText(spannableStringBuilder);
								textView.getParent().requestDisallowInterceptTouchEvent(true);//不允许父view拦截
                                downX = (int) event.getX();
                                downY = (int) event.getY();
                                break;
                            }
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int currentX = (int) event.getX();
                        int currentY = (int) event.getY();
                        if (Math.abs(currentX-downX) < slop && Math.abs(currentY-downY) < slop) {
                            break;
                        }
						textView.getParent().requestDisallowInterceptTouchEvent(false);//允许父view拦截
                    case MotionEvent.ACTION_UP:
                        spannableStringBuilder.removeSpan(span);
                        textView.setText(spannableStringBuilder);
                        int upX = (int) event.getX();
                        int upY = (int) event.getY();
                        if (Math.abs(upX-downX) < slop && Math.abs(upY-downY) < slop) {
                            //TODO startActivity or whatever
                            if (downSection != null) {
                                String name = source.substring(downSection.start,downSection.end);
                                Toast.makeText(context,name,Toast.LENGTH_SHORT).show();
                            }
                        }
                        break;
                }
                return true;
            }
        });
        return spannableStringBuilder;
    }
}
