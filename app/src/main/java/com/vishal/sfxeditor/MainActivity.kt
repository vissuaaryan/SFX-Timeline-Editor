package com.vishal.sfxeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import java.io.FileInputStream

class MainActivity:ComponentActivity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{SfxApp()}} }

@OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SfxApp(){
 val ctx=androidx.compose.ui.platform.LocalContext.current
 var page by remember{mutableStateOf("home")};var script by remember{mutableStateOf("")};var style by remember{mutableStateOf("premium_cinematic")};var lang by remember{mutableStateOf("Hindi")};var prompt by remember{mutableStateOf("")}
 var raw by remember{mutableStateOf("")};var video by remember{mutableStateOf<Uri?>(null)};var videoName by remember{mutableStateOf("No video selected")};var project by remember{mutableStateOf<SfxProject?>(null)};var assets by remember{mutableStateOf(listOf<AssetSlot>())};var events by remember{mutableStateOf(listOf<AudioEvent>())};var pending by remember{mutableStateOf<String?>(null)};var status by remember{mutableStateOf("Ready")};var exporting by remember{mutableStateOf(false)};var progress by remember{mutableFloatStateOf(0f)}
 fun persist(uri:Uri){try{ctx.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}}
 val videoPick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u->if(u!=null){video=u;persist(u);videoName=displayName(ctx,u);status="Video imported"}}
 val jsonPick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u->if(u!=null)try{ctx.contentResolver.openInputStream(u)?.bufferedReader()?.use{raw=it.readText()};persist(u);status="JSON loaded"}catch(e:Exception){status="JSON read failed: ${e.message}"}}
 val audioPick=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){u->if(u!=null&&pending!=null){persist(u);assets=assets.map{if(it.id==pending)it.copy(uri=u)else it};pending=null;status="Audio asset assigned"}}
 val bulkPick=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){list->if(list.isNotEmpty()){list.forEach{persist(it)};val pool=list.toMutableList();assets=assets.map{a->if(a.uri!=null)a else {val exact=pool.indexOfFirst{displayName(ctx,it).equals(a.filename,true)};val byId=if(exact<0)pool.indexOfFirst{displayName(ctx,it).startsWith(a.id,true)}else exact;if(byId>=0)a.copy(uri=pool.removeAt(byId))else a}};status="Bulk matching finished. Review unmatched assets."}}
 val exportPick=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")){dest->val p=project;val v=video;if(dest!=null&&p!=null&&v!=null){exporting=true;progress=0f;TimelineExporter(ctx).export(p.copy(assets=assets,events=events),v,object:TimelineExporter.Callback{override fun onProgress(percent:Int){(ctx as? ComponentActivity)?.runOnUiThread{progress=percent/100f}};override fun onComplete(file:File){Thread{try{ctx.contentResolver.openOutputStream(dest)?.use{o->FileInputStream(file).use{it.copyTo(o)}};(ctx as? ComponentActivity)?.runOnUiThread{exporting=false;status="MP4 export complete"}}catch(e:Exception){(ctx as? ComponentActivity)?.runOnUiThread{exporting=false;status="Save failed: ${e.message}"}}}.start()};override fun onError(message:String){(ctx as? ComponentActivity)?.runOnUiThread{exporting=false;status="Export failed: $message"}}})}}
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF69E7B9),secondary=Color(0xFF80CFFF),background=Color(0xFF090F14),surface=Color(0xFF14202A))){
  Scaffold(containerColor=Color(0xFF090F14),topBar={TopAppBar(title={Column{Text("SFX TIMELINE EDITOR",fontWeight=FontWeight.Bold);Text("AI-assisted cinematic sound design",fontSize=11.sp,color=Color.LightGray)}},colors=TopAppBarDefaults.topAppBarColors(containerColor=Color(0xFF101A22),titleContentColor=Color.White))},bottomBar={NavigationBar(containerColor=Color(0xFF101A22)){listOf("home" to "Home","get" to "Get Code","import" to "I Have Code","timeline" to "Timeline").forEach{(k,n)->NavigationBarItem(selected=page==k,onClick={page=k},icon={Icon(when(k){"home"->Icons.Default.Home;"get"->Icons.Default.AutoAwesome;"import"->Icons.Default.Code;else->Icons.Default.GraphicEq},null)},label={Text(n,fontSize=10.sp)})}}}){pad->
   when(page){
    "home"->LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{Text("Your sound design workspace",fontSize=24.sp,color=Color.White,fontWeight=FontWeight.Bold);Text("Script → AI JSON → Assets → Timeline → MP4",color=Color.LightGray)};item{HomeTile("GET CODE","Create a strict AI video-analysis prompt",{page="get"})};item{HomeTile("I HAVE CODE","Import JSON and your original video",{page="import"})};item{HomeTile("TIMELINE & EXPORT","Review audio events and render final MP4",{page="timeline"})};item{Text("${project?.name?:"No project loaded"}\n$videoName\n${assets.count{it.uri!=null}} / ${assets.size} assets assigned\n$status",color=Color.LightGray)}}
    "get"->LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("GET CODE",fontSize=24.sp,color=Color.White,fontWeight=FontWeight.Bold);Text("Paste script. Attach the actual video when using the generated prompt in an external AI.",color=Color.LightGray,fontSize=12.sp);OutlinedTextField(script,{script=it},modifier=Modifier.fillMaxWidth(),minLines=5,label={Text("Video script")})};item{Text("SOUND STYLE",color=Color.White,fontWeight=FontWeight.Bold);listOf("premium_cinematic","dark_intense","modern_tech","minimal").forEach{Row(Modifier.fillMaxWidth().clickable{style=it},verticalAlignment=Alignment.CenterVertically){RadioButton(style==it,{style=it});Text(it.replace("_"," "),color=Color.White)}}};item{Text("OUTPUT LANGUAGE",color=Color.White);Row{listOf("Hindi","Hinglish","English").forEach{Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically){RadioButton(lang==it,{lang=it});Text(it,color=Color.White,fontSize=12.sp)}}};Button(onClick={prompt=MasterPrompt.build(script,style,lang);status="Prompt generated"},enabled=script.isNotBlank(),modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.AutoAwesome,null);Text("Generate Master Prompt")}};if(prompt.isNotBlank())item{OutlinedTextField(prompt,{},modifier=Modifier.fillMaxWidth(),minLines=9,readOnly=true);Button(onClick={copyText(ctx,prompt);status="Prompt copied"},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.ContentCopy,null);Text("Copy Prompt")}}}
    "import"->LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("I HAVE CODE",fontSize=24.sp,color=Color.White,fontWeight=FontWeight.Bold);Button(onClick={videoPick.launch(arrayOf("video/*"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.VideoFile,null);Text("Select Original Video")};Text(videoName,color=Color(0xFF69E7B9),fontSize=12.sp);OutlinedButton(onClick={jsonPick.launch(arrayOf("application/json","text/*","*/*"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.FileOpen,null);Text("Import JSON File")};OutlinedTextField(raw,{raw=it},modifier=Modifier.fillMaxWidth(),minLines=7,label={Text("Or paste JSON")});Button(onClick={try{val p=ProjectParser.parse(raw);project=p;assets=p.assets;events=p.events;status="Valid JSON: ${p.assets.size} assets, ${p.events.size} events"}catch(e:Exception){status="Validation error: ${e.message}"}},enabled=raw.isNotBlank(),modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Verified,null);Text("Validate JSON & Create Slots")}};if(assets.isNotEmpty()){item{Text("ASSET MANAGER · ${assets.count{it.uri!=null}}/${assets.size}",color=Color(0xFF69E7B9),fontWeight=FontWeight.Bold);OutlinedButton(onClick={bulkPick.launch(arrayOf("audio/*"))},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.LibraryMusic,null);Text("Bulk Import Audio Files")}};items(assets){AssetCard(it){pending=it.id;audioPick.launch(arrayOf("audio/*"))}}};item{Text(status,color=Color.LightGray,fontSize=12.sp)}}
    else->TimelinePage(Modifier.padding(pad),ctx,project,video,videoName,assets,events,{events=it},{videoPick.launch(arrayOf("video/*"))},{bulkPick.launch(arrayOf("audio/*"))},{id->pending=id;audioPick.launch(arrayOf("audio/*"))},exporting,progress,{exportPick.launch("SFX_Final_Mix.mp4")},status)
   }
  }
 }
}

@Composable fun HomeTile(title:String,desc:String,action:()->Unit){Card(onClick=action,colors=CardDefaults.cardColors(containerColor=Color(0xFF182A32))){Column(Modifier.fillMaxWidth().padding(16.dp)){Text(title,color=Color.White,fontWeight=FontWeight.Bold);Text(desc,color=Color.LightGray,fontSize=12.sp);Text("OPEN →",color=Color(0xFF69E7B9),fontWeight=FontWeight.Bold)}}}
fun displayName(ctx:android.content.Context,u:Uri):String=ctx.contentResolver.query(u,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else u.lastPathSegment?:"file"}?:u.lastPathSegment?:"file"
fun copyText(ctx:android.content.Context,s:String){val c=ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager;c.setPrimaryClip(android.content.ClipData.newPlainText("SFX Master Prompt",s))}
@Composable fun AssetCard(a:AssetSlot,onPick:()->Unit){Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF18232C))){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.AudioFile,null,tint=Color(0xFF69E7B9));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(a.filename,color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold);Text("${a.id} · ${a.type}",color=Color.LightGray,fontSize=10.sp)};Column(horizontalAlignment=Alignment.End){Text(if(a.uri!=null)"ADDED" else "MISSING",color=if(a.uri!=null)Color(0xFF69E7B9)else Color(0xFFFFC46B),fontSize=10.sp);TextButton(onClick=onPick){Text(if(a.uri!=null)"Replace" else "Upload")}}}}}

@OptIn(UnstableApi::class)
@Composable fun TimelinePage(mod:Modifier,ctx:android.content.Context,project:SfxProject?,video:Uri?,videoName:String,assets:List<AssetSlot>,events:List<AudioEvent>,onEvents:(List<AudioEvent>)->Unit,onVideo:()->Unit,onBulk:()->Unit,onPick:(String)->Unit,exporting:Boolean,progress:Float,onExport:()->Unit,status:String){
 val player=remember(video){if(video==null)null else ExoPlayer.Builder(ctx).build().apply{setMediaItem(MediaItem.fromUri(video));prepare()}};DisposableEffect(player){onDispose{player?.release()}}
 LazyColumn(mod.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Text("TIMELINE & EXPORT",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold);if(player!=null)AndroidView(factory={PlayerView(it).apply{this.player=player;useController=true}},update={it.player=player},modifier=Modifier.fillMaxWidth().height(210.dp))else OutlinedButton(onClick=onVideo,modifier=Modifier.fillMaxWidth()){Text("Select Video")};Text(videoName,color=Color.LightGray,fontSize=11.sp)}
  if(project==null)item{Text("Import and validate JSON first.",color=Color(0xFFFFC46B))}else{
   item{Text("${project.name} · ${project.durationMs} ms · ${project.fps} FPS",color=Color.LightGray,fontSize=12.sp);OutlinedButton(onClick=onBulk,modifier=Modifier.fillMaxWidth()){Text("Bulk Match Audio Files")};Text("ASSETS",color=Color(0xFF69E7B9),fontWeight=FontWeight.Bold)}}
   items(assets){AssetCard(it){onPick(it.id)}}
   if(project!=null){item{Text("TIMELINE EVENTS",color=Color(0xFF69E7B9),fontWeight=FontWeight.Bold)}}
   items(events){e->Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF18232C))){Column(Modifier.padding(12.dp)){Text("${e.id} · ${assets.firstOrNull{it.id==e.assetId}?.filename?:e.assetId}",color=Color.White,fontWeight=FontWeight.Bold,fontSize=12.sp);Text("${e.startMs}ms → ${e.endMs}ms · Layer ${e.layer}",color=Color.LightGray,fontSize=11.sp);Text(e.visualAction.ifBlank{e.reason},color=Color.Gray,fontSize=10.sp);Row(verticalAlignment=Alignment.CenterVertically){Text("Volume",color=Color.LightGray,fontSize=11.sp);Slider(e.volume,{v->onEvents(events.map{if(it.id==e.id)it.copy(volume=v)else it})},modifier=Modifier.weight(1f));Text("${(e.volume*100).toInt()}%",color=Color.White,fontSize=10.sp)}}}}
   if(project!=null)item{val missing=assets.count{it.required&&it.uri==null};Text("Required assets missing: $missing",color=if(missing==0)Color(0xFF69E7B9)else Color(0xFFFFC46B));Button(onClick=onExport,enabled=video!=null&&missing==0&&!exporting,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Movie,null);Text("Render Final MP4")};if(exporting)LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth())}
   item{Text(status,color=Color.LightGray,fontSize=11.sp);Text("Renderer uses AndroidX Media3 Composition to mix the source video and synchronized audio sequences. Crossfades between tracks are not supported by Media3 Composition; fade envelopes on individual clips are applied.",color=Color.Gray,fontSize=10.sp)}
 }
}

object MasterPrompt {
 fun build(script:String,style:String,language:String)= """
# ROLE: PROFESSIONAL CINEMATIC SOUND DESIGNER & FRAME-ACCURATE AUDIO EDITOR

## SCRIPT
${script.trim()}

## SETTINGS
Sound style: $style
Output language: $language
Target: SFX Timeline Editor Android app.

## VIDEO ANALYSIS
Analyze the entire attached motion-graphics video, not the script alone. The script is narrative context; the video is the source of truth for visual actions and timestamps. Inspect all scenes, animation entrances/exits, typography, transitions, camera moves, object movements, impacts, reveals and ending. Determine actual duration, FPS and resolution when available. Do not invent visual events or timestamps. If frame inspection is unavailable, disclose this in analysis.limitations and lower timing confidence.

## SOUND DESIGN
Create restrained premium cinematic documentary sound. Choose only meaningful effects: deep/soft whooshes, cinematic impacts, risers, reverse swells, atmospheric beds, mechanical movement, reveal accents and optional music. Avoid cheap stock sounds, cartoon effects, repetitive whooshes, excessive bass, unnecessary layers and sounds masking narration. Do not force background music.

## ASSET NAMING
Use unique sequential IDs and uppercase filenames with underscores: SFX_001_DEEP_WHOOSH.wav, MUSIC_001_BACKGROUND_SCORE.wav, AMB_001_VILLAGE_AMBIENCE.wav. Reuse an asset ID if the same file is reused. Include detailed generation_prompt, description, category, required and suggested_duration_ms for each asset. Do not generate audio files.

## TIMING
All timestamps are integer milliseconds. Include start_ms, end_ms, duration_ms, source_frame. Use the actual video FPS. duration_ms must equal end_ms-start_ms. Events must fit video duration. Never claim frame accuracy without frame inspection. Preserve silence when no sound is needed.

## MIXING
Every event needs volume 0..1, fade_in_ms, fade_out_ms, pan -1..1, layer, priority and ducking_enabled. Avoid clipping, preserve narration clarity, control low frequencies and use smooth fades. Suggested online target approximately -14 LUFS and max -1 dBTP; these are targets, not measured results.

## STRICT JSON OUTPUT
Return ONLY one valid JSON object, no Markdown fences, no commentary. Follow this exact top-level structure:
{
 "schema_version":"1.0",
 "project":{"project_name":"Motion Graphics Sound Design","video_filename":"","video_duration_ms":0,"fps":0,"resolution":"","language":"$language","sound_style":"$style"},
 "analysis":{"video_analyzed":true,"script_analyzed":true,"frame_inspection_available":true,"timing_method":"frame_based","limitations":[]},
 "audio_settings":{"preserve_original_audio":true,"sample_rate":48000,"master_volume":1.0,"target_lufs":-14,"true_peak_db":-1},
 "assets":[{"id":"SFX_001","filename":"SFX_001_DEEP_WHOOSH.wav","type":"sfx","category":"whoosh","description":"","required":true,"suggested_duration_ms":1200,"generation_prompt":"","visual_reason":""}],
 "timeline":[{"event_id":"EVENT_001","asset_id":"SFX_001","start_ms":2400,"end_ms":3600,"duration_ms":1200,"source_frame":72,"layer":1,"volume":0.75,"fade_in_ms":100,"fade_out_ms":200,"pan":0.0,"priority":"medium","ducking_enabled":false,"visual_action":"","reason":""}],
 "mixing":{"music_ducking":true,"prevent_clipping":true,"normalization":"measured_loudness","master_fade_out_ms":0},
 "validation":{"all_asset_references_valid":true,"timestamps_within_video":true,"duplicate_ids":false,"json_valid":true,"warnings":[]}
}

## VALIDATION
IDs unique; every timeline asset_id must exist; start >= 0; end > start; duration exact; volume 0..1; pan -1..1; fades non-negative and total fade duration <= event duration; no event beyond video duration; no unnecessary duplicate assets. Empty arrays are allowed. Report unknown facts in limitations. Analyze the attached video now and return only the import-ready JSON object.
""".trimIndent()
}
