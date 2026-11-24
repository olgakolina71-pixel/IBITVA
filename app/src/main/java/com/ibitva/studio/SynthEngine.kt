package com.ibitva.studio
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import kotlin.math.*
data class Note(val step:Int,val pitch:Int,val length:Int=1,val wave:Int=0,val velocity:Float=.78f)
object SynthEngine {
 const val SR=44100
 fun hz(m:Int)=440.0*2.0.pow((m-69)/12.0)
 fun sample(m:Int,t:Double,d:Double,w:Int,v:Float=.8f):Float {
  val ph=2*PI*hz(m)*t
  val raw=when(w){1->if(sin(ph)>=0)1.0 else -1.0;2->2*(ph/(2*PI)-floor(ph/(2*PI)+.5));3->asin(sin(ph))*2/PI;else->sin(ph)}
  val env=min(1.0,t/.012)* (if(t<.08)1.0 else .72+.28*exp(-(t-.08)*3))*min(1.0,(d-t)/.07).coerceAtLeast(0.0)
  return (raw*env*v*.24).toFloat()
 }
 fun render(notes:List<Note>,bpm:Int,seconds:Double):ByteArray {
  val frames=(SR*seconds).toInt();val pcm=ShortArray(frames*2)
  notes.forEach { n-> val st=n.step*60.0/bpm/4;val len=n.length*60.0/bpm/4
   val a=(st*SR).toInt().coerceAtLeast(0);val z=((st+len)*SR).toInt().coerceAtMost(frames)
   for(i in a until z){val x=(pcm[2*i]/32768.0+sample(n.pitch,(i-a).toDouble()/SR,len,n.wave,n.velocity)).coerceIn(-.95,.95).toInt();val q=(x*32767).toShort();pcm[2*i]=q;pcm[2*i+1]=q}
  }
  val b=ByteArray(pcm.size*2);pcm.forEachIndexed{i,s->b[2*i]=s.toByte();b[2*i+1]=(s.toInt() shr 8).toByte()};return b
 }
 fun play(notes:List<Note>,bpm:Int){Thread{
  val dur=max(4.0,notes.maxOfOrNull{it.step+it.length}?.let{it*60.0/bpm/4}?:4.0);val data=render(notes,bpm,dur)
  val at=AudioTrack(AudioManager.STREAM_MUSIC,SR,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT,data.size,AudioTrack.MODE_STATIC)
  at.write(data,0,data.size);at.play();Thread.sleep((data.size/4.0/SR*1000).toLong()+80);at.stop();at.release()
 }.start()}
 fun wav(p:ByteArray):ByteArray {
  val h=ByteArray(44);fun ascii(i:Int,s:String){s.forEachIndexed{j,c->h[i+j]=c.code.toByte()}}
  fun i32(i:Int,v:Int){for(k in 0..3)h[i+k]=(v shr (8*k)).toByte()}
  fun i16(i:Int,v:Int){h[i]=v.toByte();h[i+1]=(v shr 8).toByte()}
  ascii(0,"RIFF");i32(4,36+p.size);ascii(8,"WAVE");ascii(12,"fmt ");i32(16,16);i16(20,1);i16(22,2);i32(24,SR);i32(28,SR*4);i16(32,4);i16(34,16);ascii(36,"data");i32(40,p.size);return h+p
 }
}
