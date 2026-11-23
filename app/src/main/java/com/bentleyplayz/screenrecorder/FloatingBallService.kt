package com.bentleyplayz.screenrecorder

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.os.Build
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatingBallService:Service(){
 companion object{private const val CHANNEL="bentley_floating";private const val NOTIFICATION_ID=7102}
 private lateinit var wm:WindowManager
 private var ball:TextView?=null
 private var menu:LinearLayout?=null
 private lateinit var lp:WindowManager.LayoutParams
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{if(intent?.action=="com.bentleyplayz.screenrecorder.HIDE_BALL"){stopSelf();return START_NOT_STICKY};return START_STICKY}
 override fun onCreate(){
  super.onCreate()
  if(!Settings.canDrawOverlays(this)){stopSelf();return}
  if(Build.VERSION.SDK_INT>=26)(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL,"Floating recorder controls",NotificationManager.IMPORTANCE_LOW))
  val hide=PendingIntent.getService(this,72,Intent(this,FloatingBallService::class.java).setAction("com.bentleyplayz.screenrecorder.HIDE_BALL"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notice=Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("BentleyPlayz floating controls").setContentText("BP quick controls are available on screen.").addAction(Notification.Action.Builder(null,"Hide",hide).build()).setOngoing(true).build()
  if(Build.VERSION.SDK_INT>=34)startForeground(NOTIFICATION_ID,notice,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)else startForeground(NOTIFICATION_ID,notice)
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  val size=dp(58)
  lp=WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=dp(18);y=dp(190)}
  val v=TextView(this).apply{text="BP";textSize=17f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setBackgroundResource(android.R.drawable.btn_default);background.setTint(Color.rgb(78,111,255));elevation=dp(12).toFloat()}
  var downX=0f;var downY=0f;var originX=0;var originY=0;var dragged=false
  v.setOnTouchListener{view,event->when(event.action){
   MotionEvent.ACTION_DOWN->{downX=event.rawX;downY=event.rawY;originX=lp.x;originY=lp.y;dragged=false;true}
   MotionEvent.ACTION_MOVE->{val dx=(event.rawX-downX).toInt();val dy=(event.rawY-downY).toInt();if(abs(dx)>5||abs(dy)>5)dragged=true;lp.x=originX+dx;lp.y=originY+dy;wm.updateViewLayout(view,lp);true}
   MotionEvent.ACTION_UP->{if(!dragged)toggleMenu();true}
   else->false
  }}
  ball=v;wm.addView(v,lp)
 }
 private fun toggleMenu(){if(menu!=null){removeMenu();return}
  val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),dp(10),dp(10),dp(10));setBackgroundColor(Color.rgb(24,31,53));elevation=dp(16).toFloat()}
  panel.addView(menuButton("START RECORDING"){requestRecordingStart()})
  panel.addView(menuButton("OPEN RECORDER"){openRecorder()})
  panel.addView(menuButton("PAUSE / RESUME"){send(RecorderService.ACTION_TOGGLE_PAUSE)})
  panel.addView(menuButton("STOP RECORDING"){send(RecorderService.ACTION_STOP)})
  panel.addView(menuButton("HIDE FLOATING BALL"){stopSelf()})
  val mp=WindowManager.LayoutParams(dp(210),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.START;x=(lp.x-dp(145)).coerceAtLeast(0);y=(lp.y+dp(66)).coerceAtLeast(0)}
  menu=panel;wm.addView(panel,mp)
 }
 private fun menuButton(text:String,action:()->Unit)=TextView(this).apply{textSize=13f;this.text=text;gravity=Gravity.CENTER_VERTICAL;setTextColor(Color.WHITE);setPadding(dp(12),dp(12),dp(12),dp(12));setOnClickListener{removeMenu();action()}}
 private fun removeMenu(){menu?.let{if(it.isAttachedToWindow)wm.removeView(it)};menu=null}
 private fun requestRecordingStart(){
  val launch=Intent(this,CapturePermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
  startActivity(launch)
 }
 private fun openRecorder(){startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))}
 private fun send(action:String){startService(Intent(this,RecorderService::class.java).setAction(action))}
 override fun onDestroy(){removeMenu();ball?.let{if(it.isAttachedToWindow)wm.removeView(it)};ball=null;super.onDestroy()}
 override fun onBind(intent:Intent?):IBinder?=null
}