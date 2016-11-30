package com.junyou.hbks;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

//import com.switfpass.pay.MainApplication;
//import com.switfpass.pay.activity.PayPlugin;
//import com.switfpass.pay.bean.RequestMsg;
import com.junyou.hbks.apppayutils.ComFunction;
import com.junyou.hbks.apppayutils.WXPayUtil;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.umeng.analytics.MobclickAgent;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.junyou.hbks.wxapi.WXUtil;

public class MainActivity extends AppCompatActivity implements AccessibilityManager.AccessibilityStateChangeListener
{
    private AccessibilityManager accessibilityManager;
    SharedPreferences sharedPreferences;
    RelativeLayout mainLayoutHeader;
    private static MainActivity instance;

    private static final String DATE_MARK = "date_mark";                //日期记录
    private static final String FIRST_DATE_MARK = "first_date_mark";    //第一次进来的时间，只保存一次
    private static final int BORN_DAYS = 2;                             //初始天数

    private Switch openWechat_switch;
    private Switch openQQ_switch;

    //左上角两个个按钮
    private ImageButton setting_imagebtn;
    private ImageButton help_imagebtn;

    private RelativeLayout shouldOpenServer_layout;

    private ImageView top_image;

    private TextView wechat_auto_text;
    private TextView qq_auto_text;

    //红包个数 金额标签
    public TextView num_redpkt;
    public TextView num_money;

    //跑马灯文本
    private TextView marquee_text;

    //剩余天数
    public TextView left_days_text;
    //弹窗
    private Dialog dialog_openSvs;
    private Dialog dialog_openShare;
    public  Dialog dialog_receiveTime;
    private Dialog dialog_tryDays;
    private Dialog dialog_open_vip;

    //广播消息
    private Intent bor_intent;

    //sdk 相关
    private IWXAPI wxAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
//        Log.i("TAG", "onCreate<<<<<<");
        setContentView(R.layout.activity_main);
        instance = this;
        regToWx();      //注册微信id
        WXPayUtil.init(this);
        //友盟埋点
        MobclickAgent.setDebugMode(true);   //打开友盟埋点数据统计测试
        MobclickAgent.setScenarioType(instance, MobclickAgent.EScenarioType.E_UM_NORMAL);
        UmengUtil.YMPhoneInfo(this);

        //监听AccessibilityService 变化
        accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.addAccessibilityStateChangeListener(this);

        mainLayoutHeader = (RelativeLayout) findViewById(R.id.layout_header);
        /*
        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedP.edit();
        editor.putBoolean(Constants.IS_ALLLIFEUSE,true);    //终身使用
        editor.apply();
       */
        //-----------------------new items--------------------------//
        //开关
        openWechat_switch = (Switch) findViewById(R.id.open_wechat_switch);
        if (openWechat_switch != null){
            openWechat_switch.setOnCheckedChangeListener(wechat_swtich_listener);
        }
        openQQ_switch = (Switch) findViewById(R.id.open_qq_switch);
        if (openQQ_switch != null){
            openQQ_switch.setOnCheckedChangeListener(qq_switch_listener);
        }

        wechat_auto_text = (TextView)findViewById(R.id.wechat_auto);
        qq_auto_text = (TextView) findViewById(R.id.qq_auto);

        //设置和帮助按钮
        setting_imagebtn = (ImageButton) findViewById(R.id.imageButton_setting);
        if (setting_imagebtn != null){
            setting_imagebtn.setOnClickListener(onClickSetting);
        }
        help_imagebtn = (ImageButton) findViewById(R.id.imageButton_help);
        if (help_imagebtn != null){
            help_imagebtn.setOnClickListener(onClickHelp);
        }
        //顶部图片
        top_image = (ImageView) findViewById(R.id.top_img_show);

        //红包个数标签 金额标签
        num_redpkt = (TextView) findViewById(R.id.packt_num_text);
        num_money = (TextView) findViewById(R.id.money_num_text);

        //剩余天数标签
        left_days_text = (TextView) findViewById(R.id.left_days_text);

        //布局获取
        shouldOpenServer_layout = (RelativeLayout) findViewById(R.id.should_openServer);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //跑马灯文本
        marquee_text = (TextView) findViewById(R.id.marquee_text);
        //广播
       bor_intent = new Intent("junyou.com.hbtools.RECEIVER");

        updateServiceStatus();
        showDatas();
        refrishMarqueeText();
        showDialog();
        //showLeftDays();
        showSwitchStatus();
//      openNotifocation();
//        setCurTime("");
        TimeManager.init(this);
        newShowLeftDays();
        initTime();
        new TimeThread().start();
}

    class TimeThread extends Thread {
        @Override
        public void run() {
            do {
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = 1;  //消息(一个整型值)
                    mHandler.sendMessage(msg);// 每隔1秒发送一个msg给mHandler
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (true);
        }
    }
    //在主线程里面处理消息并更新UI界面
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    SharedPreferences sharedP = getSharedPreferences("config",MODE_MULTI_PROCESS);
                    if (sharedP.getBoolean(Constants.IS_ALLLIFEUSE,false)){
                        //终身使用
                        setCurTime(getResources().getString(R.string.forever));
                        return;
                    }
                    int totalTime = Integer.valueOf(TimeManager.getLeftTime());
                    int useTime = Integer.valueOf(TimeManager.getDiffTime());
                    int leftTime = totalTime - useTime;
//                    Log.i("TAG","总时间:" + totalTime + "  使用时间:" + useTime + "  剩余时间:" + TimeManager.minutesToDays(""+ leftTime));
                    setCurTime("" + TimeManager.minutesToDays(""+ leftTime)); //更新时间
                     if (TimeManager.isTimeout()){
                         //没有时间了
                         setCurTime( "0天" +"0小时"+"0分钟");
//                         Log.i("TAG","没有时间了");
                     }
                    break;
            }
        }
    };

    private void setCurTime(CharSequence curtime) {
        if (null != left_days_text){
            left_days_text.setText(curtime);
        }
    }

    private void initTime(){
        SharedPreferences sharedP = getSharedPreferences("config",MODE_PRIVATE);
        if (sharedP.getBoolean(Constants.IS_ALLLIFEUSE,false)){
            //终身使用
            setCurTime(getResources().getString(R.string.forever));
            return;
        }
        int totalTime = Integer.valueOf(TimeManager.getLeftTime());
        int useTime = Integer.valueOf(TimeManager.getDiffTime());
        int leftTime = totalTime - useTime;
//      Log.i("TAG","总时间:" + totalTime + "  使用时间:" + useTime + "  剩余时间:" + TimeManager.minutesToDays(""+ leftTime));
        setCurTime("" + TimeManager.minutesToDays(""+ leftTime)); //更新时间
        if (TimeManager.isTimeout()){
            //没有时间了
            setCurTime( "0天" +"0小时"+"0分钟");
        }

    }
    private CharSequence getSystemTime() {
        long sysTime = System.currentTimeMillis();
        return  DateFormat.format("yyyy-MM-dd HH:mm:ss",sysTime);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openNotifocation()
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder builder = new Notification.Builder(this);

        PendingIntent contentIndent = PendingIntent.getActivity(this, 0, new Intent(this,MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIndent)
                .setSmallIcon(R.mipmap.ic_launcher)//设置状态栏里面的图标（小图标)
                .setWhen(System.currentTimeMillis())//设置时间发生时间
                .setAutoCancel(true)//设置可以清除
                .setContentTitle("红包快手未开启")//设置下拉列表里的标题
                .setContentText("亲,不能抢红包了!");//设置上下文内容

        try{
            if (!isServiceEnabled()){
                notificationManager.notify(1,builder.build());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //updateServiceStatus();
        //showLeftDays();
//        Log.i("TAG", "onStart<<<<<<");
    }

    //注册微信id
    private void regToWx()
    {
        wxAPI = WXAPIFactory.createWXAPI(this,Constants.APP_ID,true);
        wxAPI.registerApp(Constants.APP_ID);
    }

    private void showSwitchStatus()
    {
        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
        boolean wechat_data = sharedP.getBoolean("wechat_switch",true);
        boolean qq_data = sharedP.getBoolean("qq_switch",true);

        if (wechat_data) {
//            Log.i("TAG", "微信开");
            try {
                openWechat_switch.setChecked(true);
                wechat_auto_text.setText("自动抢   开启");
                wechat_auto_text.setTextColor(getResources().getColor(R.color.colortextyellow));
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            try {
                openWechat_switch.setChecked(false);
                wechat_auto_text.setText("自动抢   关闭");
                wechat_auto_text.setTextColor(getResources().getColor(R.color.colortextblue));
            }catch (Exception e){
                e.printStackTrace();
            }
//            Log.i("TAG", "微信关");
        }

        if (qq_data){
            try {
                openQQ_switch.setChecked(true);
                qq_auto_text.setText("自动抢   开启");
                qq_auto_text.setTextColor(getResources().getColor(R.color.colortextyellow));
            }catch (Exception e){
                e.printStackTrace();
            }
//            Log.i("TAG", "QQ开");
        }else{
            try {
                openQQ_switch.setChecked(false);
                qq_auto_text.setText("自动抢   关闭");
                qq_auto_text.setTextColor(getResources().getColor(R.color.colortextblue));
            }catch (Exception e){
                e.printStackTrace();
            }
//            Log.i("TAG", "QQ关");
        }
    }

    private void refrishMarqueeText()
    {
        final String []marquee_lists = {
                getResources().getString(R.string.marquee_word_1),
                getResources().getString(R.string.marquee_word_2),
                getResources().getString(R.string.marquee_word_3),
                getResources().getString(R.string.marquee_word_4),
                getResources().getString(R.string.marquee_word_5),
                getResources().getString(R.string.marquee_word_6),
                getResources().getString(R.string.marquee_word_7),
                getResources().getString(R.string.marquee_word_8),
                getResources().getString(R.string.marquee_word_9),
                getResources().getString(R.string.marquee_word_10),
                getResources().getString(R.string.marquee_word_11),
                getResources().getString(R.string.marquee_word_12)
        };
        //调度器
        Timer timer = new Timer();
        final Handler handler = new Handler(){
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                    {
                        int num = (int)(Math.random()*12);  //0-11
                        if (null != marquee_text)
                        {
                            marquee_text.setText(marquee_lists[num]);
                        }
                    }
                    break;
                }
                super.handleMessage(msg);
            }
        };

        TimerTask task = new TimerTask(){
            public void run() {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
            }
        };
        timer.schedule(task, 20000,20000);    //20秒之后执行，每20秒执行一次
    }

    private void showDialog()
    {
        //打开设置弹窗
        View view_1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_openservice, null);
        dialog_openSvs = new Dialog(this,R.style.common_dialog);
        if (dialog_openSvs != null){
            dialog_openSvs.setContentView(view_1);
        }
        //打开分享弹窗
        View view_2 = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_share,null);
        dialog_openShare = new Dialog(this,R.style.common_dialog);
        if (dialog_openShare != null){
            dialog_openShare.setContentView(view_2);
        }

        //主页的收到天数弹窗
        View view_3 = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_receivetime,null);
        dialog_receiveTime = new Dialog(this,R.style.common_dialog);
        if (dialog_receiveTime != null){
            dialog_receiveTime.setContentView(view_3);
        }
        //dialog_receiveTime.show();

        //刚启动 赠送天数弹窗
        SharedPreferences sharedP = getSharedPreferences("config",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedP.edit();
        int days = getSharedPreferences("config",MODE_PRIVATE).getInt("showTryDaysDialog",-1);
        if (days <0){
            View view_4 = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_trydays,null);
            dialog_tryDays = new Dialog(this,R.style.common_dialog);
            if (dialog_tryDays != null){
                dialog_tryDays.setContentView(view_4);
                dialog_tryDays.show();
            }
            editor.putInt("showTryDaysDialog",1);
            editor.apply();
        }

        View view_5 = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_supervip, null);
        dialog_open_vip = new Dialog(this,R.style.common_dialog);
        if (dialog_open_vip != null){
            dialog_open_vip.setContentView(view_5);
//            dialog_open_vip.show();
        }
    }

    //右下角显示剩余的天数
    private void newShowLeftDays(){

        SharedPreferences sharedP = getSharedPreferences("config",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedP.edit();

        if (sharedP.getBoolean(Constants.IS_ALLLIFEUSE,false)){
            setCurTime(getResources().getString(R.string.forever));
            return;
        }

        if (TimeManager.isNewDayFirstEnter()){
            //是否新的一天
            int use_day = TimeManager.getUseDay();
            if (use_day % 3 == 0){
                //每三天弹一次窗
                if (dialog_open_vip != null){
                    dialog_open_vip.show();
                }
            }
        }

        if (TimeManager.isTimeout()){
            //没有时间了
            TimeManager.setServiceOnOrOff(false);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.shareForDays), Toast.LENGTH_LONG).show();
        }else{
            TimeManager.setServiceOnOrOff(true);
        }
    }

    //右下角显示剩余的天数
    private void showLeftDays()
    {
        SharedPreferences sharedP = getSharedPreferences("config",MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedP.edit();

        //设置天数
        /**
         * 若为-99  设置为3天
         * 若不为-99  获得之前设置的天数
         */
        int days = getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.LEFT_DAYS_COUNT,-99);
        if (days == -99 )
        {
//            Log.i("TAG", "天数小于0。。。");
            editor.putInt(Constants.LEFT_DAYS_COUNT,BORN_DAYS);
            editor.apply();
            if (left_days_text != null)
            {
                int days_1 = getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.LEFT_DAYS_COUNT,0);
                left_days_text.setText(String.valueOf(days_1) + " " + getResources().getString(R.string.days));
            }
        }else
        {
//            Log.i("TAG", ",天数有值。。。");
            if (left_days_text != null)
            {
                int days_2 = getSharedPreferences("config",MODE_MULTI_PROCESS).getInt(Constants.LEFT_DAYS_COUNT,BORN_DAYS);
//                int days_2 = getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.LEFT_DAYS_COUNT,BORN_DAYS);
                Log.i("TAG", "leftDays:" + days_2);
                left_days_text.setText(String.valueOf(days_2) + " " + getResources().getString(R.string.days));
            }
        }

        if (sharedP.getBoolean(Constants.IS_ALLLIFEUSE,false)){
            if (left_days_text != null)
            {
                left_days_text.setText(getResources().getString(R.string.forever));
            }
            return;
        }

        /**
         * 1. 第一次进入app,获取保存的时间，若为空，则保存现在的时间
         *     若不为空，拿当前的系统时间和保存的时间比较
         *     若相等，则是同一天，不相等，则是新的一天
         *  2.  若是新的一天  将剩余天数减 1  改掉UI显示
         *      若不是新的一天  不操作
         */

        //判断是否新的一天
//        Calendar calendar = Calendar.getInstance();
        /*
        //for test
        String nowDate = calendar.get(Calendar.YEAR) + "年"
                + (calendar.get(Calendar.MONTH)+1) + "月"//从0计算
                + calendar.get(Calendar.DAY_OF_MONTH) + "日"
                + calendar.get(Calendar.HOUR_OF_DAY) + "时"
                + calendar.get(Calendar.MINUTE)+ "分"
                + calendar.get(Calendar.SECOND)+ "秒";
*/
//        String nowDate = calendar.get(Calendar.YEAR) + "年"
//                + (calendar.get(Calendar.MONTH)+1) + "月"//从0计算
//                + calendar.get(Calendar.DAY_OF_MONTH) + "日";

        String nowDate = TimeUtils.getCurTime();  //2016-10-09  //获取当前时间
        String defaultTime = getSharedPreferences("config",MODE_PRIVATE).getString(DATE_MARK,"empty");
        if ("empty".equals(defaultTime)){
            editor.putString(DATE_MARK,nowDate);
            editor.apply();
            editor.putString(FIRST_DATE_MARK,nowDate);      //标记一次时间
            editor.apply();
            editor.putBoolean(Constants.IS_SERVICE_ON,true);    //服务开启
            editor.apply();
            editor.putBoolean(Constants.IS_NEW_DAY,true);       //是新的一天
            editor.apply();
            editor.putInt(Constants.USE_DAY,1);                 //使用的天数
            editor.apply();
//            Log.i("TAG", "<<<第一次进来,日期为empty,我保存到了本地");
        }else{
           String saveTime =  getSharedPreferences("config",MODE_PRIVATE).getString(DATE_MARK,"empty");
            if (nowDate.equals(saveTime)) {
//                Log.i("TAG","<<<不是新的一天");
                editor.putBoolean(Constants.IS_SERVICE_ON,true);
                editor.apply();
            }else
            {
//                Log.i("TAG","<<<是新的一天");        //todo 一天只能走一次
                editor.putBoolean(Constants.IS_NEW_DAY,true);
                editor.apply();

                int use_day = getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.USE_DAY,0);
                editor.putInt(Constants.USE_DAY,use_day +1);
                editor.apply();

                 int save_day = getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.USE_DAY,1);
                if (save_day % 3 == 0){
                    //每三天弹一次窗
                    if (dialog_open_vip != null){
                        //购买vip弹窗
                        dialog_open_vip.show();
                    }
                }
//                Log.i("TAG", "使用天数:" + getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.USE_DAY,1));
                //todo 减去使用天数  saveDays应该是第一次进来的时间，只保存一次
//                String saveDays = getSharedPreferences("config",MODE_PRIVATE).getString(FIRST_DATE_MARK,"empty");
//                int  useDays = TimeUtils.friendly_time(saveDays);   //使用天数
//                Log.i("TAG", "使用天数:" + useDays +" ??????");
//                int  useDays = TimeUtils.friendly_time("2016-10-9");   //使用天数
                //todo  useDays时间减少得有问题
//                int days_5 = getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.LEFT_DAYS_COUNT,0) - useDays;
                int days_5 = getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.LEFT_DAYS_COUNT,0) - 1;
//                Log.i("TAG", "剩余天数:" + days_5 +" ??????");
                if (days_5 >=0){
                    editor.putInt(Constants.LEFT_DAYS_COUNT,days_5);
                    editor.apply();
//                    Log.i("TAG", "设置的天数" + getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.LEFT_DAYS_COUNT,0));
                    if (left_days_text != null)
                    {
                        left_days_text.setText(getSharedPreferences("config",MODE_PRIVATE).getInt(Constants.LEFT_DAYS_COUNT,0) + getResources().getString(R.string.days));
                    }

                    if (days_5 == 0){
                        editor.putBoolean(Constants.IS_SERVICE_ON,false);
                        editor.apply();
//                        if (dialog_open_vip != null){
//                        dialog_open_vip.show();
//                        }
                        //todo 没有天数
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.shareForDays), Toast.LENGTH_LONG).show();
//                        Log.i("TAG", "没有天数了 days_5==0");
                    }else{
                        editor.putBoolean(Constants.IS_SERVICE_ON,true);
                        editor.apply();
                    }
                }else{
                    //没有天数了，需要一个弹窗提醒
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.shareForDays), Toast.LENGTH_LONG).show();
//                    Log.i("TAG", "没有天数了");
                    //todo  没有天数
//                    if (dialog_open_vip != null){
//                        dialog_open_vip.show();
//                    }
                    editor.putBoolean(Constants.IS_SERVICE_ON,false);
                    editor.apply();
                }

                editor.putString(DATE_MARK,nowDate);        //第二天第一次进来，存值，的二天下次进来不减时间
                editor.apply();
            }
        }
    }

    public void showDatas()
    {
        SharedPreferences sharedP=  getSharedPreferences("config",MODE_MULTI_PROCESS);  //不同进程之间可以访问
//        Log.i("TAG", "初始总红包数量:"+ String.valueOf(sharedP.getInt("totalnum",0)));
//        Log.i("TAG", "初始总资产:"+ sharedP.getString("totalmoney","0.00"));
        //显示数据
        if (num_redpkt != null){
            num_redpkt.setText(String.valueOf(sharedP.getInt("totalnum",0)));
        }

        if (num_money != null){
            num_money.setText(sharedP.getString("totalmoney","0.00"));
        }

//        if ("".equals(sharedP.getString("totalmoney","")))
//        {
//            if (num_money != null)
//                num_money.setText(sharedP.getString("totalmoney","0.00"));
//        }else
//        {
//            if (num_money != null)
//            num_money.setText(sharedP.getString("totalmoney","0.00"));
//        }
    }

    public static MainActivity getInstance()
    {
        if (instance != null){
            return instance;
        }
        return  null;
    }

    private View.OnClickListener onClickSetting = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
//            Log.i("TAG","setting");
            try {
                Intent settingAvt = new Intent(MainActivity.this,SettingActivity.class);
                settingAvt.putExtra("title", "设置");
                settingAvt.putExtra("frag_id", "GeneralSettingsFragment");
                startActivity(settingAvt);
                UmengUtil.YMclk_setting(MainActivity.this);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private  View.OnClickListener onClickShare = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
//            Log.i("TAG","share");
        }
    };

    private  View.OnClickListener onClickHelp = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            try {
                Intent helpAvt = new Intent(MainActivity.this,helpActivity.class);
                startActivity(helpAvt);
//                Log.i("TAG","help");
                UmengUtil.YMclk_help(MainActivity.this);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //移除监听服务
        if (accessibilityManager != null){
            accessibilityManager.removeAccessibilityStateChangeListener(this);
        }
    }
    //todo error
    @Override
    protected void onResume()
    {
        super.onResume();
        updateServiceStatus();
        showDatas();
       // showLeftDays();
        newShowLeftDays();
//        Log.i("TAG", "OnResume<<<<<<");
        MobclickAgent.onPageStart("MainActivity");  //统计页面
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
       //updateServiceStatus();
       // showLeftDays();
        newShowLeftDays();
//        Log.i("TAG", "onRestart<<<<<<");
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd("MainActivity");    //统计页面
        MobclickAgent.onPause(this);
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled)
    {
        updateServiceStatus();
    }

    private void updateServiceStatus()
    {
        if (isServiceEnabled())
        {
            Log.i("TAG","service is on");
//            Toast.makeText(getApplicationContext(), "红包快手已经开启", Toast.LENGTH_SHORT).show();
            if (shouldOpenServer_layout != null){
                shouldOpenServer_layout.setVisibility(View.INVISIBLE);
            }
            if (top_image != null){
                top_image.setImageResource(R.mipmap.top_img_radpacket_yes);
            }
            showSwitchStatus();
            if (mainLayoutHeader != null){
                mainLayoutHeader.setBackgroundColor(getResources().getColor(R.color.mainbgOn));
            }
        } else
        {
            Log.i("TAG","service is off");
//            Toast.makeText(getApplicationContext(), "红包快手已经关闭", Toast.LENGTH_SHORT).show();
            if (shouldOpenServer_layout != null){
                shouldOpenServer_layout.setVisibility(View.VISIBLE);
            }
            if (top_image != null){
                top_image.setImageResource(R.mipmap.top_img_radpacket_on);
            }
            showSwitchStatus();
            if (mainLayoutHeader != null){
                mainLayoutHeader.setBackgroundColor(getResources().getColor(R.color.mainbgOff));
            }
        }
    }

    private View.OnClickListener myClickListener = new View.OnClickListener()
    {
        public void onClick(View v)
        {
            try
            {
//                Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//                startActivity(accessibleIntent);
            } catch (Exception e)
            {
                Toast.makeText(getApplicationContext(), "遇到一些问题,请手动打开系统设置>辅助服务>微信红包助手", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    };

    private boolean isServiceEnabled()
    {
        List<AccessibilityServiceInfo> accessibilityServices =
                accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices)
        {
            if (info.getId().equals(getPackageName() + "/.RobMoney"))
            {
//                Log.i("TAG", "服务开");
                return true;
            }
        }
//        Log.i("TAG", "服务关");
        return false;
    }

    private CompoundButton.OnCheckedChangeListener wechat_swtich_listener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isServiceEnabled())
            {
                //服务已经开启
                if (isChecked)
                {
                    //打开开关
                    //发送广播
                    if (bor_intent != null){
                        bor_intent.putExtra("wechat_broadcast", true);
                        sendBroadcast(bor_intent);
                    }
                    if (wechat_auto_text != null){
                        wechat_auto_text.setText("自动抢   开启");
                        wechat_auto_text.setTextColor(getResources().getColor(R.color.colortextyellow));
                    }

                    try {
                        //  存数据
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putBoolean("wechat_switch",true);
                        editor.apply();

                        if (sharedP.getBoolean("wechat_switch",true))
                        {
//                            Log.i("TAG", "手动设置了微信开");
                        }else
                        {
//                            Log.i("TAG", "不能手动设置微信开");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }else
                {
                    //关闭开关
                    if (bor_intent != null){
                        bor_intent.putExtra("wechat_broadcast", false);
                        sendBroadcast(bor_intent);
                    }
                    if (wechat_auto_text != null){
                        wechat_auto_text.setText("自动抢   关闭");
                        wechat_auto_text.setTextColor(getResources().getColor(R.color.colortextblue));
                    }

                    try {
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putBoolean("wechat_switch",false);
                        editor.apply();

                        if (!sharedP.getBoolean("wechat_switch",true))
                        {
//                            Log.i("TAG", "手动设置了微信关");
                        }else
                        {
//                            Log.i("TAG", "不能手动设置微信关");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }else
            {
                //服务已经关闭
                if (isChecked)
                {
                    //未开启服务 弹出提示，再进入设置
                    if (null != dialog_openSvs)
                    {
                        dialog_openSvs.show();
                        UmengUtil.YMclk_fuzhu(MainActivity.this);
                    }
                    if (wechat_auto_text != null){
                        wechat_auto_text.setText("自动抢   关闭");
                        wechat_auto_text.setTextColor(getResources().getColor(R.color.colortextblue));
                    }
                    try {
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putBoolean("wechat_switch",true);
                        editor.apply();
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }else
                {
                    if (wechat_auto_text != null){
                        wechat_auto_text.setText("自动抢   关闭");
                        wechat_auto_text.setTextColor(getResources().getColor(R.color.colortextblue));
                    }
                    try {
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putBoolean("wechat_switch",false);
                        editor.apply();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private CompoundButton.OnCheckedChangeListener qq_switch_listener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isServiceEnabled())
            {
                if (isChecked)
                {
                    if (bor_intent != null){
                        bor_intent.putExtra("qq_broadcast", true);
                        sendBroadcast(bor_intent);
                    }
                    if (qq_auto_text != null){
                        qq_auto_text.setText("自动抢   开启");
                        qq_auto_text.setTextColor(getResources().getColor(R.color.colortextyellow));
                    }
                    try {
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putBoolean("qq_switch",true);
                        editor.apply();

                        if (sharedP.getBoolean("qq_switch",true))
                        {
//                            Log.i("TAG", "手动设置了qq开");
                        }else
                        {
//                            Log.i("TAG", "不能手动设置qq开");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else
                {
                    if (bor_intent != null){
                        bor_intent.putExtra("qq_broadcast", false);
                        sendBroadcast(bor_intent);
                    }
                    if (qq_auto_text != null){
                        qq_auto_text.setText("自动抢   关闭");
                        qq_auto_text.setTextColor(getResources().getColor(R.color.colortextblue));
                    }
                    try {
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putBoolean("qq_switch",false);
                        editor.apply();

                        if (!sharedP.getBoolean("qq_switch",true))
                        {
//                            Log.i("TAG", "手动设置了qq关");
                        }else
                        {
//                            Log.i("TAG", "不能手动设置qq关");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }else
            {
                if (isChecked)
                {
                    //未开启服务 弹出提示，再进入设置
                    if (null != dialog_openSvs)
                    {
                        dialog_openSvs.show();
                        UmengUtil.YMclk_fuzhu(MainActivity.this);
                    }
                    if (qq_auto_text != null){
                        qq_auto_text.setText("自动抢   关闭");
                        qq_auto_text.setTextColor(getResources().getColor(R.color.colortextblue));
                    }
                    try {
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putBoolean("qq_switch",true);
                        editor.apply();

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }else
                {
                    if (qq_auto_text != null){
                        qq_auto_text.setText("自动抢   关闭");
                        qq_auto_text.setTextColor(getResources().getColor(R.color.colortextblue));
                    }
                    try {
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putBoolean("qq_switch",false);
                        editor.apply();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    public void openSettings(View view)
    {
        if (!isServiceEnabled())
        {
            if (null != dialog_openSvs)
            {
                dialog_openSvs.show();
            }
            UmengUtil.YMclk_fuzhu(MainActivity.this);
            //未开启服务 弹出提示，再进入设置
            /*
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("必须打开辅助功能->红包快手->开启服务，才能抢红包哦.")
                    .setPositiveButton("去打开辅助功能", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try
                            {
                                Log.i("TAG", "打开了设置");
                                Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                startActivity(accessibleIntent);
                            } catch (Exception e)
                            {
                                Toast.makeText(getApplicationContext(), "遇到一些问题,请手动打开系统设置>辅助服务>微信红包助手", Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    })
                    .setNegativeButton("取消",null)
                    .show();
*/
        }
    }

    //右下角获取更多天数按钮
    public void getMoreTime(View view)
    {
//        Log.i("TAG", "点我获取天数哦");
        if (null != dialog_openShare)
            dialog_openShare.show();
        UmengUtil.YMclk_share(this);
    }

    public void openServiceClick(View view)
    {
//        Log.i("TAG", "点击打开系统设置");
        try
        {
            Intent accessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            if(null != accessibleIntent){
                startActivity(accessibleIntent);
            }
        } catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), "遇到一些问题,请手动打开系统设置>辅助服务>微信红包助手", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        if (null != dialog_openSvs)
        {
            dialog_openSvs.dismiss();
        }
    }

    public void closeOpenServiceClick(View view)
    {
//        Log.i("TAG", "点击关闭系统设置提示");
        if (null != dialog_openSvs)
        {
            dialog_openSvs.dismiss();
        }
    }

    public void closeOpenShare(View view)
    {
        if (null != dialog_openShare)
        {
            dialog_openShare.dismiss();
        }
    }

    public void sharePengYouQuanClick(View view)
    {
//        Log.i("TAG", "点击分享到朋友圈");
        if (null != dialog_openShare)
        {
            dialog_openShare.dismiss();
            UmengUtil.YMclk_share_wctp(MainActivity.this);
        }
        final String []share_lists = {
                getResources().getString(R.string.share_1),
                getResources().getString(R.string.share_2),
                getResources().getString(R.string.share_3),
                getResources().getString(R.string.share_4)
        };
        int num = (int)(Math.random()*4);  //0-3
        //不使用sdk分享

        final String PackageName = "com.tencent.mm";
        final String ActivityName = "com.tencent.mm.ui.tools.ShareToTimeLineUI"; //微信朋友圈
        if (ShareHelper.isInstalled(this,PackageName,ActivityName)){
            //图片加文字
            /*
            Bitmap bt= BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher);
            final Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bt, null,null));
            Intent intent = new Intent();
            ComponentName comp = new ComponentName(PackageName, ActivityName);//带图片分享
            intent.setComponent(comp);
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra("Kdescription", "红包快手，让红包来的容易点~~");
            startActivity(intent);
            */
            //使用sdk分享
            WXWebpageObject webpage = new WXWebpageObject();
//            webpage.webpageUrl = "http://www.zjhzjykj.com/images/hbks.apk";     //网址替换掉就可以了
//            webpage.webpageUrl = "http://www.wandoujia.com/apps/cn.swiftpass.wxpay";     //todo 网址替换掉就可以了

           // webpage.webpageUrl = "http://info.appstore.vivo.com.cn/detail/1643019?source=7";//vivo
            webpage.webpageUrl = "http://www.zjhzjykj.com";
            WXMediaMessage msg = new WXMediaMessage(webpage);
            msg.title = "红包快手 "+share_lists[num];
            msg.description = share_lists[num];
            Bitmap thumb = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            msg.thumbData = WXUtil.bmpToByteArray(thumb, true);

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = ShareHelper.buildTransaction("webpage");
            req.message = msg;
            req.scene = SendMessageToWX.Req.WXSceneTimeline;    //朋友圈
            wxAPI.sendReq(req);
        }else {
            Toast.makeText(getApplicationContext(), "您没有安装微信", Toast.LENGTH_SHORT).show();
        }
    }

    public void shareWeiXinClick(View view)
    {
//        Log.i("TAG", "点击分享给微信朋友");
        if (null != dialog_openShare)
        {
            dialog_openShare.dismiss();
            UmengUtil.YMclk_share_wct(MainActivity.this);
        }
        final String []share_lists = {
                getResources().getString(R.string.share_1),
                getResources().getString(R.string.share_2),
                getResources().getString(R.string.share_3),
                getResources().getString(R.string.share_4)
        };
        int num = (int)(Math.random()*4);  //0-3

        final String PackageName = "com.tencent.mm";
        final String ActivityName = "com.tencent.mm.ui.tools.ShareImgUI";
        if (ShareHelper.isInstalled(this,PackageName,ActivityName)){

            //文字或链接
            /*
            Intent intent = new Intent();
            ComponentName comp = new ComponentName(PackageName, ActivityName);
            intent.setComponent(comp);
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "红包快手，让红包来的容易点~~");
            startActivity(intent);
               */
            //图片
            /*
            Bitmap bt= BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher);
            final Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bt, null,null));
            Intent intent = new Intent();
            ComponentName comp = new ComponentName(PackageName, ActivityName);
            intent.setComponent(comp);
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("image/*");        //分享图片，没有图片用转回到分享文字
            intent.putExtra(Intent.EXTRA_STREAM,uri);
            startActivity(intent);
            */
            //使用sdk分享
            WXWebpageObject webpage = new WXWebpageObject();
//            webpage.webpageUrl = "http://www.zjhzjykj.com/images/hbks.apk";
//            webpage.webpageUrl = "http://www.wandoujia.com/apps/cn.swiftpass.wxpay";  //todo 换网址
            //webpage.webpageUrl = "http://info.appstore.vivo.com.cn/detail/1643019?source=7";   //vivo
            webpage.webpageUrl = "http://www.zjhzjykj.com";
            WXMediaMessage msg = new WXMediaMessage(webpage);
            msg.title = "红包快手";
            msg.description = share_lists[num];
            Bitmap thumb = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            msg.thumbData = WXUtil.bmpToByteArray(thumb, true);

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = ShareHelper.buildTransaction("webpage");
            req.message = msg;
            req.scene = SendMessageToWX.Req.WXSceneSession;     //好友
            wxAPI.sendReq(req);
        }else {
            Toast.makeText(getApplicationContext(), "您没有安装微信", Toast.LENGTH_SHORT).show();
        }
    }

    public void shareQQClick(View view)
    {
//        Log.i("TAG", "点击分享给QQ好友");
        if (null != dialog_openShare)
        {
            dialog_openShare.dismiss();
        }
          final String PackageName = "com.tencent.mobileqq";
          final String ActivityName = "com.tencent.mobileqq.activity.JumpActivity"; //qq好友
         if (ShareHelper.isInstalled(this,PackageName,ActivityName)){
             //分享文字给好友
             Intent intent = new Intent(Intent.ACTION_SEND);
             ComponentName component = new ComponentName(PackageName,ActivityName);
             intent.setComponent(component);
             intent.putExtra(Intent.EXTRA_TEXT, "红包快手，让红包来的容易点~");
             intent.setType("text/plain");
             startActivity(intent);
         }else {
             Toast.makeText(getApplicationContext(), "您没有安装手机QQ", Toast.LENGTH_SHORT).show();
         }
        //todo  分享到qq空间
    }

    public void shareWeiboClick(View view)
    {
//        Log.i("TAG", "点击分享到微博");
        if (null != dialog_openShare)
        {
            dialog_openShare.dismiss();
        }

        if (isSinaWiBoAvilible(this))
        {
            //分享文字
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            String pakName = "com.sina.weibo";
            intent.setPackage(pakName);
            intent.putExtra(Intent.EXTRA_TEXT, "红包快手，让红包来的容易点~");
            this.startActivity(Intent.createChooser(intent, ""));

            /*
            //图片加文字
            Bitmap bt= BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher);
            final Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bt, null,null));
            Intent intent = new Intent();
            intent.setPackage("com.sina.weibo");
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, "红包快手，让红包来的容易点~");
            startActivity(intent);
            */
        }else
        {
            Toast.makeText(getApplicationContext(), "您没有安装新浪微博", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isSinaWiBoAvilible(Context context)
    {
        final PackageManager packageManager = context.getPackageManager();// 获取packagemanager
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);// 获取所有已安装程序的包信息
        if (pinfo != null) {
            for (int i = 0; i < pinfo.size(); i++)
            {
                String pn = pinfo.get(i).packageName;
                if (pn.equals("com.sina.weibo"))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void closeReceiveTime(View view)
    {
//        Log.i("TAG", "关闭收到天数啦");
        if (dialog_receiveTime != null){
            dialog_receiveTime.dismiss();
        }
    }

    public void receive_confirm_click(View view)
    {
//        Log.i("TAG", "确定收到天数");
        if (dialog_receiveTime != null){
            dialog_receiveTime.dismiss();
        }
    }

    public void receive_getmore_click(View view)
    {
//        Log.i("TAG", "成为超级VIP");
        if (dialog_receiveTime != null){
            dialog_receiveTime.dismiss();
        }
        //todo 直接进入微信支付6.66元
        if (ComFunction.networkInfo(this)){
            if (ComFunction.isWechatAvilible(this)){
                try{
                    if (null != WXPayUtil.getInstance()){
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putString(Constants.MONEY_NUM,"666");
                        //editor.putString(Constants.MONEY_NUM,"1");
                        editor.commit();
                        WXPayUtil.getInstance().new GetPrepayIdTask().execute();
                    }
                    UmengUtil.YMpurchase_num(this);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
                Toast.makeText(getApplicationContext(), "您未安装微信!", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getApplicationContext(), "网络未连接!", Toast.LENGTH_SHORT).show();
        }
    }

    public void try_days_click(View view){
        if (dialog_tryDays != null){
            dialog_tryDays.dismiss();
        }
    }

    public void closeTryDays(View view){
        if (dialog_tryDays != null){
            dialog_tryDays.dismiss();
        }
    }

    public void super_vip_click(View view)
    {
        if (dialog_open_vip != null){
            dialog_open_vip.dismiss();
        }
        //todo 直接进入微信支付6.66元
        if (ComFunction.networkInfo(this)){
            if (ComFunction.isWechatAvilible(this)){
                try{
                    if (null != WXPayUtil.getInstance()){
                        SharedPreferences sharedP=  getSharedPreferences("config",MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedP.edit();
                        editor.putString(Constants.MONEY_NUM,"666");
//                        editor.putString(Constants.MONEY_NUM,"1");
                        editor.commit();
                        WXPayUtil.getInstance().new GetPrepayIdTask().execute();
                    }
                    UmengUtil.YMpurchase_num(this);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
                Toast.makeText(getApplicationContext(), "您未安装微信!", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getApplicationContext(), "网络未连接!", Toast.LENGTH_SHORT).show();
        }
//        Log.i("TAG", "点击获取超级VIP");
    }

    public void closeOpenSuperVip(View view)
    {
        if (dialog_open_vip != null){
            dialog_open_vip.dismiss();
        }
    }
}
