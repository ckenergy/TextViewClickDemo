package cn.cc.net.textviewclickdemo;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.MotionEventCompat;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//TODO callback
public class WeiBoContentTextUtil {

    private static class Section {
        static final int AT = 1;
        static final int TOPIC = 2;// ##话题

        private int start;
        private int end;
        private int type;
        private String name;


        Section(int start, int end, int type, String name) {
            this.start = start;
            this.end = end;
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString() {
            return "start:"+start+",end:"+end;
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

        /*if (matcher.find()) {
            if (!(textView instanceof EditText)) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setFocusable(false);
                textView.setClickable(false);
                textView.setLongClickable(false);
            }
            matcher.reset();
        }*/

        while (matcher.find()) {
            final String at = matcher.group(1);
            final String topic = matcher.group(2);

            //处理@用户
            if (at != null) {
                int start = matcher.start(1);
                int end = start + at.length();
                Section section = new Section(start,end,Section.AT,at);
                sections.add(section);
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.BLUE);
                spannableStringBuilder.setSpan(foregroundColorSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            //处理##话题
            if (topic != null) {
                int start = matcher.start(2);
                int end = start + topic.length();
                Section section = new Section(start,end,Section.TOPIC,topic);
                sections.add(section);
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.BLUE);
                spannableStringBuilder.setSpan(foregroundColorSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }

        }
        final BackgroundColorSpan span = new BackgroundColorSpan(Color.YELLOW);
        final int slop = ViewConfiguration.get(context).getScaledTouchSlop();
        textView.setOnTouchListener(new View.OnTouchListener() {

            int downX,downY;
            Section downSection = null;
            int id;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);
                Layout layout = textView.getLayout();
                if (layout == null) {
                    Log.d(TAG,"layout is null");
                    return false;
                }
                int line = 0;
                int index = 0;

                switch(action) {
                    case MotionEvent.ACTION_DOWN://TODO 最后一行点击问题 网址链接
                        int actionIndex = event.getActionIndex();
                        id = event.getPointerId(actionIndex);
                        downX = (int) event.getX(actionIndex);
                        downY = (int) event.getY(actionIndex);
                        Log.d(TAG, "ACTION_down,x:"+event.getX()+",y:"+event.getY());
                        line = layout.getLineForVertical(textView.getScrollY() + (int)event.getY());
                        index = layout.getOffsetForHorizontal(line, (int)event.getX());
                        int lastRight = (int) layout.getLineRight(line);
                        Log.d(TAG,"lastRight:"+lastRight);
                        if (lastRight < event.getX()) {  //文字最后为话题时，如果点击在最后一行话题之后，也会造成话题被选中效果
                            return false;
                        }
                        Log.d(TAG," index:"+ index+",sections:"+sections.size());
                        for (Section section : sections) {
                            if (index >= section.start &&  index <= section.end) {
                                spannableStringBuilder.setSpan(span, section.start, section.end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                                downSection = section;
                                textView.setText(spannableStringBuilder);
                                textView.getParent().requestDisallowInterceptTouchEvent(true);//不允许父view拦截
                                Log.d(TAG,"downSection"+downSection.toString());
                                return true;
                            }
                        }

                        return false;
                    case MotionEvent.ACTION_MOVE:
                        int indexMove = event.findPointerIndex(id);
                        int currentX = (int) event.getX(indexMove);
                        int currentY = (int) event.getY(indexMove);
                        Log.d(TAG, "ACTION_MOVE,x:"+currentX+",y:"+currentY);
                        if (Math.abs(currentX-downX) < slop && Math.abs(currentY-downY) < slop) {
                            if (downSection == null) {
                                Log.d(TAG, "downSection is null");
                                textView.getParent().requestDisallowInterceptTouchEvent(false);//允许父view拦截
                                return false;
                            }

                            break;
                        }
                        downSection = null;
                        textView.getParent().requestDisallowInterceptTouchEvent(false);//允许父view拦截

                    case MotionEvent.ACTION_CANCEL:
                        Log.d(TAG, "ACTION_CANCEL");
                    case MotionEvent.ACTION_UP:
                        int indexUp = event.findPointerIndex(id);
                        spannableStringBuilder.removeSpan(span);
                        textView.setText(spannableStringBuilder);
                        int upX = (int) event.getX(indexUp);
                        int upY = (int) event.getY(indexUp);
                        Log.d(TAG, "ACTION_UP,x:"+upX+",y:"+upY);
                        if (Math.abs(upX-downX) < slop && Math.abs(upY-downY) < slop) {
                            //TODO startActivity or whatever
                            if (downSection != null) {
                                String name = downSection.name;
                                Toast.makeText(context,name,Toast.LENGTH_SHORT).show();
                                downSection = null;
                            }else {
                                return false;
                            }
                        }else {
                            Log.d(TAG, "false");
                            downSection = null;
                            return false;
                        }
                        break;
                }
                Log.d(TAG,"true");
                return true;
            }
        });
        return spannableStringBuilder;
    }

}
