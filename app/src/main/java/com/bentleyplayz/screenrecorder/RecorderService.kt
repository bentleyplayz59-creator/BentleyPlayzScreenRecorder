package com.bentleyplayz.screenrecorder
import android.app.*
import android.content.*
import android.hardware.display.*
import android.media.*
import android.media.projection.*
import android.os.*
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.min

class RecorderService:Service(){
 companion object{
  const val ACTION_START="com.bentleyplayz.screenrecorder.START"
  const val ACTION_STOP="com.bentleyplayz.screenrecorder.STOP"
  const val ACTION_TOGGLE_PAUSE="com.bentleyplayz.screenrecorder.TOGGLE_PAUSE"
  const val EXTRA_RESULT_CODE="resultCode";const val EXTRA_DATA="captureData"
  private const val CHANNEL="bentley_recorder";private const val NOTIFICATION_ID=7101
 }
 private val running=AtomicBoolean(false)
 @Volatile private var paused=false
 private var projection:MediaProjection?=null
 private var virtualDisplay:VirtualDisplay?=null
 private var videoCodec:MediaCodec?=null;private var audioCodec:MediaCodec?=null
 private var muxer:MediaMuxer?=null;private var videoTrack=-1;private var audioTrack=-1;private var muxerStarted=false
 private val muxLock=Any();private val pending=ArrayList<Sample>()
 private var outputFile:File?=null;private var audioThread:Thread?=null;private var videoThread:Thread?=null
 private var mic:AudioRecord?=null;private var playback:AudioRecord?=null;private var surface:Surface?=null
 private data class Sample(val video:Boolean,val data:ByteArray,val pts:Long,val flags:Int)
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26)(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL,"Screen recording",NotificationManager.IMPORTANCE_LOW))}
 override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int{when(i?.action){ACTION_START->startCapture(i);ACTION_STOP->stopCapture();ACTION_TOGGLE_PAUSE->togglePause()};return START_NOT_STICKY}

 private fun startCapture(i:Intent){
  if(running.get())return
  val code=i.getIntExtra(EXTRA_RESULT_CODE,0)
  val data=if(Build.VERSION.SDK_INT>=33)i.getParcelableExtra(EXTRA_DATA,Intent::class.java)else @Suppress("DEPRECATION") i.getParcelableExtra(EXTRA_DATA) as? Intent
  if(code==0||data==null){stopSelf();return}
  try{
   val note=notification("Preparing capture")
   if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,note,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)else startForeground(NOTIFICATION_ID,note)
   projection=(getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(code,data)
   projection?.registerCallback(object:MediaProjection.Callback(){override fun onStop(){stopCapture()}},Handler(mainLooper))
   setupEncoders()
   running.set(true);paused=false
   startAudio()
   videoThread=thread(name="BPVideoDrain"){drainVideo()}
   updateNote("Recording screen, internal audio and microphone")
  }catch(e:Exception){android.util.Log.e("BPRecorder","Start failed",e);stopCapture()}
 }

 private fun setupEncoders(){
  val quality=getSharedPreferences("recorder",MODE_PRIVATE).getInt("quality",1)
  val dm=DisplayMetrics()
  @Suppress("DEPRECATION") val display=(getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
  display.getRealMetrics(dm)
  val scale=if(quality==0)min(720f/dm.heightPixels,1f)else min(1080f/dm.heightPixels,1f)
  val w=((dm.widthPixels*scale).toInt()/2)*2;val h=((dm.heightPixels*scale).toInt()/2)*2
  val fps=if(quality==2)60 else 30;val rate=if(quality==0)4_000_000 else if(quality==2)12_000_000 else 8_000_000
  val vf=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,w.coerceAtLeast(2),h.coerceAtLeast(2))
  vf.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);vf.setInteger(MediaFormat.KEY_BIT_RATE,rate);vf.setInteger(MediaFormat.KEY_FRAME_RATE,fps);vf.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2)
  videoCodec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply{configure(vf,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);surface=createInputSurface();start()}
  val af=MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,44100,1)
  af.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);af.setInteger(MediaFormat.KEY_BIT_RATE,128000);af.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,16384)
  audioCodec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply{configure(af,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);start()}
  val dir=File(getExternalFilesDir(null),"Recordings").apply{mkdirs()}
  val stamp=SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(Date());outputFile=File(dir,"BP_Recording_$stamp.mp4")
  muxer=MediaMuxer(outputFile!!.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
  virtualDisplay=projection?.createVirtualDisplay("BentleyPlayzRecorder",w.coerceAtLeast(2),h.coerceAtLeast(2),dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,surface,null,null)
 }

 private fun startAudio(){
  val rate=44100;val minSize=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
  if(minSize<=0)throw IllegalStateException("PCM audio input unavailable")
  val size=minSize.coerceAtLeast(8192)*2
  mic=AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size)
  if(mic?.state!=AudioRecord.STATE_INITIALIZED)throw IllegalStateException("Microphone initialization failed")
  if(Build.VERSION.SDK_INT>=29)try{
   val cfg=AudioPlaybackCaptureConfiguration.Builder(projection!!)
    .addMatchingUsage(AudioAttributes.USAGE_MEDIA).addMatchingUsage(AudioAttributes.USAGE_GAME).addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).build()
   playback=AudioRecord.Builder().setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()).setBufferSizeInBytes(size).setAudioPlaybackCaptureConfig(cfg).build()
   if(playback?.state!=AudioRecord.STATE_INITIALIZED){playback?.release();playback=null}
  }catch(e:Exception){android.util.Log.w("BPRecorder","Playback capture unavailable; microphone remains active",e);playback=null}
  mic?.startRecording();playback?.startRecording()
  audioThread=thread(name="BPAudioMixer"){mixAudio(size)}
 }

 private fun mixAudio(size:Int){
  val a=ShortArray(size/2);val b=ShortArray(size/2);val out=ShortArray(size/2);val info=MediaCodec.BufferInfo()
  var pts=System.nanoTime()/1000L
  while(running.get()){
   if(paused){SystemClock.sleep(25);continue}
   val na=try{mic?.read(a,0,a.size)?:0}catch(_:Exception){0}
   val nb=try{playback?.read(b,0,b.size)?:0}catch(_:Exception){0}
   if(na<=0&&nb<=0){SystemClock.sleep(5);continue}
   val n=maxOf(na,nb).coerceAtMost(out.size)
   for(x in 0 until n){out[x]=((if(x<na)a[x].toInt()else 0)+(if(x<nb)b[x].toInt()else 0)).coerceIn(-32768,32767).toShort()}
   val c=audioCodec?:break;val ix=c.dequeueInputBuffer(10000)
   if(ix>=0){val buf=c.getInputBuffer(ix);if(buf!=null){buf.clear();for(x in 0 until n){val v=out[x].toInt();buf.put((v and 255).toByte());buf.put((v shr 8).toByte())};val bytes=n*2;c.queueInputBuffer(ix,0,bytes,pts,0);pts+=1_000_000L*n/44100L}else c.queueInputBuffer(ix,0,0,pts,0)}
   drainAudio(c,info,false)
  }
  try{val c=audioCodec?:return;val ix=c.dequeueInputBuffer(10000);if(ix>=0)c.queueInputBuffer(ix,0,0,pts,MediaCodec.BUFFER_FLAG_END_OF_STREAM);drainAudio(c,info,true)}catch(e:Exception){android.util.Log.w("BPRecorder","Audio finalization",e)}
 }

 private fun drainVideo(){
  val c=videoCodec?:return;val info=MediaCodec.BufferInfo()
  while(running.get())try{val ix=c.dequeueOutputBuffer(info,10000);when{ix==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED->addFormat(true,c.outputFormat);ix>=0->{writeSample(true,c,ix,info);c.releaseOutputBuffer(ix,false)}}}catch(e:Exception){android.util.Log.e("BPRecorder","Video drain error",e);break}
 }
 private fun drainAudio(c:MediaCodec,info:MediaCodec.BufferInfo,finish:Boolean){
  var go=true
  while(go){val ix=c.dequeueOutputBuffer(info,0);when{ix==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED->addFormat(false,c.outputFormat);ix>=0->{val eos=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0;writeSample(false,c,ix,info);c.releaseOutputBuffer(ix,false);if(eos)go=false};else->if(!finish||ix==MediaCodec.INFO_TRY_AGAIN_LATER)go=false}
  }
 }
 private fun addFormat(video:Boolean,format:MediaFormat){
  synchronized(muxLock){val m=muxer?:return;if(video&&videoTrack<0)videoTrack=m.addTrack(format);if(!video&&audioTrack<0)audioTrack=m.addTrack(format)
   if(!muxerStarted&&videoTrack>=0&&audioTrack>=0){m.start();muxerStarted=true;pending.forEach{s->writePending(m,s)};pending.clear()}
  }
 }
 private fun writePending(m:MediaMuxer,s:Sample){val t=if(s.video)videoTrack else audioTrack;val bb=java.nio.ByteBuffer.wrap(s.data);val bi=MediaCodec.BufferInfo().apply{set(0,s.data.size,s.pts,s.flags)};m.writeSampleData(t,bb,bi)}
 private fun writeSample(video:Boolean,c:MediaCodec,index:Int,info:MediaCodec.BufferInfo){
  if(info.size<=0||info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG!=0)return
  val src=c.getOutputBuffer(index)?:return;src.position(info.offset);src.limit(info.offset+info.size);val bytes=ByteArray(info.size);src.get(bytes)
  synchronized(muxLock){val t=if(video)videoTrack else audioTrack;if(muxerStarted&&t>=0){val bb=java.nio.ByteBuffer.wrap(bytes);val bi=MediaCodec.BufferInfo().apply{set(0,bytes.size,info.presentationTimeUs,info.flags)};muxer?.writeSampleData(t,bb,bi)}else pending.add(Sample(video,bytes,info.presentationTimeUs,info.flags))}
 }

 private fun togglePause(){if(!running.get())return;paused=!paused;try{virtualDisplay?.setSurface(if(paused)null else surface)}catch(e:Exception){android.util.Log.w("BPRecorder","Pause surface update",e)};updateNote(if(paused)"Recording paused"else"Recording resumed")}
 private fun stopCapture(){
  if(!running.getAndSet(false)){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return}
  try{mic?.stop()}catch(_:Exception){};try{playback?.stop()}catch(_:Exception){}
  try{audioThread?.join(1800)}catch(_:Exception){};try{videoThread?.join(1200)}catch(_:Exception){}
  try{virtualDisplay?.release()}catch(_:Exception){};virtualDisplay=null
  try{projection?.stop()}catch(_:Exception){};projection=null
  try{mic?.release()}catch(_:Exception){};try{playback?.release()}catch(_:Exception){};mic=null;playback=null
  try{videoCodec?.stop()}catch(_:Exception){};try{videoCodec?.release()}catch(_:Exception){}
  try{audioCodec?.stop()}catch(_:Exception){};try{audioCodec?.release()}catch(_:Exception){};videoCodec=null;audioCodec=null
  synchronized(muxLock){try{if(muxerStarted)muxer?.stop()}catch(e:Exception){android.util.Log.w("BPRecorder","Muxer stop",e)};try{muxer?.release()}catch(_:Exception){};muxer=null;pending.clear()}
  try{surface?.release()}catch(_:Exception){};surface=null
  outputFile?.let{f->if(f.exists()&&f.length()>0){publishVideo(f);notifySaved(f)}else f.delete()}
  stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
 }
 private fun publishVideo(f:File){
  if(Build.VERSION.SDK_INT<29)return
  try{val v=ContentValues().apply{put(MediaStore.Video.Media.DISPLAY_NAME,f.name);put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/BentleyPlayz Screen Recorder");put(MediaStore.Video.Media.IS_PENDING,1)}
   val uri=contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,v)?:return
   contentResolver.openOutputStream(uri)?.use{o->FileInputStream(f).use{it.copyTo(o)}}
   v.clear();v.put(MediaStore.Video.Media.IS_PENDING,0);contentResolver.update(uri,v,null,null)
  }catch(e:Exception){android.util.Log.e("BPRecorder","Media library copy failed",e)}
 }
 private fun notifySaved(f:File){val pi=PendingIntent.getActivity(this,2,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val n=Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("Recording saved").setContentText(f.name).setContentIntent(pi).setAutoCancel(true).build();(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID+1,n)}
 private fun notification(text:String):Notification{val p=PendingIntent.getService(this,1,Intent(this,RecorderService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);return Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("BentleyPlayz Screen Recorder").setContentText(text).addAction(Notification.Action.Builder(null,"Stop",p).build()).setOngoing(true).build()}
 private fun updateNote(s:String){(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID,notification(s))}
 override fun onDestroy(){if(running.get())stopCapture();super.onDestroy()}
}
