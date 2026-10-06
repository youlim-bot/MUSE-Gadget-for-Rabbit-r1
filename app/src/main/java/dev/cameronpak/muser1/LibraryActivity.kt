package dev.cameronpak.muser1

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import kotlinx.coroutines.*
import java.util.UUID

/** Local saved copies and text memos. Explicit transcription sends audio to ElevenLabs; task extraction sends memo text to Muse. */
class LibraryActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var storage: LibraryStore
    private var saved = listOf<SavedItem>()
    private var turns = listOf<ConversationTurn>()
    private var ready = false
    private var writing = false
    private var mode = 0
    private lateinit var list: LinearLayout
    private lateinit var search: EditText
    private lateinit var state: TextView
    private var recorder: VoiceRecorder? = null
    private var memoJob: Job? = null
    private var taskJob: Job? = null
    private var taskConnection: dev.cameronpak.muser1.transport.MuseConnection? = null
    private var dialog: AlertDialog? = null
    private fun t(ko: String, ja: String, en: String) = UiText.text(this, ko, ja, en)
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false; minWidth = 0; minimumWidth = 0
        setTextColor(Color.rgb(242,233,221)); setPadding(dp(5), 0, dp(5), 0)
        background = android.graphics.drawable.InsetDrawable(android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(24).toFloat(); setColor(Color.rgb(23,24,23)); setStroke(1, Color.rgb(70,70,65))
        }, dp(3), dp(7), dp(3), dp(7))
        setOnClickListener { action() }
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        storage = LibraryStore(this)
        mode = intent.getIntExtra("mode", 0).coerceIn(0,2)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(10,11,10)); setPadding(dp(10), dp(8), dp(10), dp(8)) }
        val top = LinearLayout(this)
        top.addView(button(t("‹ 뒤로", "‹ 戻る", "‹ Back")) { finish() }, LinearLayout.LayoutParams(dp(85), dp(48)))
        top.addView(TextView(this).apply { text = t("대화 보관함", "会話ライブラリ", "Library"); textSize=17f; setTextColor(Color.WHITE); gravity=Gravity.CENTER }, LinearLayout.LayoutParams(0,dp(48),1f))
        root.addView(top)
        val tabs = LinearLayout(this)
        arrayOf(t("대화 검색", "会話検索", "Search"), t("즐겨찾기", "お気に入り", "Favorites"), t("메모·할 일", "メモ・タスク", "Memos/tasks")).forEachIndexed { index, label ->
            tabs.addView(button(label) { if (recorder == null && memoJob?.isActive != true && taskJob?.isActive != true) { mode=index; render() } }, LinearLayout.LayoutParams(0,dp(48),1f))
        }
        root.addView(tabs)
        search=EditText(this).apply { hint=t("질문·답변·메모 검색", "質問・回答・メモを検索", "Search questions, replies, memos"); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize=14f; setSingleLine(true) }
        root.addView(search,LinearLayout.LayoutParams(-1,dp(48)))
        this.state=TextView(this).apply { textSize=12f; setTextColor(Color.LTGRAY); setPadding(dp(6),dp(5),dp(6),dp(5)) }
        root.addView(this.state)
        list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) },LinearLayout.LayoutParams(-1,0,1f))
        root.addView(button(t("＋ 음성 메모", "＋ 音声メモ", "+ Voice memo")) { memoDialog() },LinearLayout.LayoutParams(-1,dp(48)))
        setContentView(root)
        search.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) = Unit
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) { if(ready) render() }
            override fun afterTextChanged(s:Editable?) = Unit
        })
        scope.launch {
            try { saved=storage.load(); turns=(application as MuseApp).displayHistory.load(); ready=true; render() }
            catch (_: Exception) { this@LibraryActivity.state.text=t("보관함을 읽지 못했습니다. 다시 열어 주세요.", "読み込めません。開き直してください。", "Could not load library. Reopen this screen.") }
        }
    }
    private fun conversationItems() = turns.asReversed().map {
        SavedItem(favoriteId(it.user.orEmpty(),it.answer),"favorite",it.user.orEmpty(),it.answer,0)
    }
    private fun render() {
        list.removeAllViews()
        val items=(if(mode==0) conversationItems() else saved.filter { if(mode==1) it.kind=="favorite" else it.kind in listOf("memo","todo") }.asReversed())
            .filter { matchesQuery(it.title,it.text,search.text.toString()) }
        state.text = t("${arrayOf("대화", "즐겨찾기", "메모")[mode]} · ${items.size}개", "${arrayOf("会話", "お気に入り", "メモ")[mode]} · ${items.size}件", "${arrayOf("Conversations", "Favorites", "Memos")[mode]} · ${items.size}")
        if(items.isEmpty()) list.addView(TextView(this).apply { text=t("표시할 항목이 없습니다.", "項目がありません。", "No matching items."); setTextColor(Color.GRAY); setPadding(dp(8),dp(20),dp(8),0) })
        items.forEach { item ->
            val star=if(saved.any { it.id==item.id && it.kind=="favorite" }) "★ " else ""
            val title=item.title.ifBlank { item.text }.replace('\n',' ').take(70)
            list.addView(button(star+title) { detail(item) }.apply { gravity=Gravity.START or Gravity.CENTER_VERTICAL; maxLines=2 },LinearLayout.LayoutParams(-1,dp(68)))
        }
    }
    private fun commit(next: List<SavedItem>, failed: () -> Unit = {}, done: () -> Unit = {}) {
        if(!ready || writing) return
        writing=true
        scope.launch {
            try { storage.save(next); saved=next; render(); done() }
            catch (_: Exception) { failed(); Toast.makeText(this@LibraryActivity,t("저장 실패", "保存失敗", "Save failed"),Toast.LENGTH_LONG).show() }
            finally { writing=false }
        }
    }
    private fun detail(item: SavedItem) {
        if(item.kind == "todo") { showChecklist(item.id); return }
        val body=TextView(this).apply { text=if(item.kind=="memo") item.text else item.title+"\n\n"+item.text; textSize=16f; setPadding(dp(18),dp(10),dp(18),dp(10)); setTextIsSelectable(true) }
        val exists=saved.any { it.id==item.id }
        val builder=AlertDialog.Builder(this).setTitle(if(item.kind=="memo") t("메모", "メモ", "Memo") else t("대화", "会話", "Conversation"))
            .setView(ScrollView(this).apply { addView(body) }).setNegativeButton(t("닫기", "閉じる", "Close"),null)
        if(item.kind=="memo") {
            builder.setPositiveButton(t("수정", "編集", "Edit")) { _,_-> editMemo(item) }
                .setNeutralButton(t("할 일로 정리", "タスクに整理", "Make to-dos")) { _,_-> summarizeMemo(item) }

        } else builder.setPositiveButton(if(exists)t("★ 해제", "★ 解除", "★ Remove") else t("☆ 즐겨찾기", "☆ 保存", "☆ Favorite")) { _,_->
            commit(if(exists)saved.filterNot { it.id==item.id } else saved+item.copy(time=System.currentTimeMillis()))
        }
        dialog=builder.show()
    }
    private fun editMemo(item: SavedItem) {
        val input=EditText(this).apply { setText(item.text); minLines=3; maxLines=7; filters=arrayOf(android.text.InputFilter.LengthFilter(8000)) }
        val d=AlertDialog.Builder(this).setTitle(t("메모 확인·수정", "メモを確認・編集", "Review memo"))
            .setView(input).setNeutralButton(t("삭제", "削除", "Delete")) { _,_-> confirmDelete(item.id) }
            .setNegativeButton(t("취소", "キャンセル", "Cancel"),null).setPositiveButton(t("저장", "保存", "Save"),null).create()
        d.setOnShowListener { d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val text=input.text.toString().trim()
            if(text.isBlank()) { input.error=t("내용을 입력하세요", "入力してください", "Enter text"); return@setOnClickListener }
            commit(saved.filterNot { it.id==item.id }+item.copy(title=text.lineSequence().first().take(80),text=text)) { d.dismiss(); mode=2; render() }
        } }; dialog=d; d.show()
    }
    private fun confirmDelete(id: String) {
        dialog=AlertDialog.Builder(this).setMessage(t("이 항목을 삭제할까요?", "この項目を削除しますか？", "Delete this item?"))
            .setNegativeButton(t("취소", "キャンセル", "Cancel"),null)
            .setPositiveButton(t("삭제", "削除", "Delete")) { _,_->commit(saved.filterNot { it.id==id }) }.show()
    }
    private fun showChecklist(id: String) {
        val item=saved.firstOrNull { it.id==id } ?: return
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),0,dp(12),0) }
        item.tasks.forEachIndexed { index, task ->
            box.addView(CheckBox(this).apply {
                text=task.text; isChecked=task.done; textSize=16f
                setOnClickListener {
                    if(writing) { isChecked=!isChecked; return@setOnClickListener }
                    val current=saved.firstOrNull { it.id==id } ?: return@setOnClickListener
                    val next=current.copy(tasks=current.tasks.mapIndexed { i,v -> if(i==index) v.copy(done=isChecked) else v })
                    commit(saved.map { if(it.id==id)next else it }, failed = { isChecked=current.tasks[index].done })
                }
            })
        }
        dialog=AlertDialog.Builder(this).setTitle(t("할 일 목록", "タスクリスト", "To-do list"))
            .setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton(t("닫기", "閉じる", "Close"),null)
            .setNeutralButton(t("삭제", "削除", "Delete")) { _,_->confirmDelete(id) }.show()
    }
    private fun summarizeMemo(item: SavedItem) {
        if(taskJob?.isActive==true || memoJob?.isActive==true) return
        val app=application as MuseApp
        val credentials=app.store.credentials() ?: return
        dialog=AlertDialog.Builder(this).setTitle(t("메모를 할 일로 정리", "メモをタスクに整理", "Turn memo into tasks"))
            .setMessage(t("이 메모를 Muse에 보내 할 일을 추립니다. 결과를 수정한 뒤 별도 목록으로 저장합니다.", "メモをMuseに送りタスクを抽出します。確認後、別のリストに保存します。", "Send this memo to Muse to extract tasks. Review before saving a separate list."))
            .setNegativeButton(t("취소", "キャンセル", "Cancel"),null)
            .setPositiveButton(t("정리하기", "整理する", "Extract tasks")) { _,_->
                val result=CompletableDeferred<String>()
                val replies=linkedMapOf<String,String>()
                val conn=dev.cameronpak.muser1.transport.MuseConnection(credentials,app.store.sdkToken(),app.store::save,
                    { }, { id,text,done -> runOnUiThread {
                        replies[id]=if(done && text.isNotEmpty()) text else replies.getOrDefault(id,"")+text
                        if(done && !result.isCompleted) result.complete(replies[id].orEmpty())
                    } }, { })
                taskConnection=conn
                state.text=t("Muse가 할 일을 정리 중…", "Museがタスクを整理中…", "Muse is extracting tasks…")
                taskJob=scope.launch {
                    try {
                        val text=withTimeout(120000) {
                            withContext(Dispatchers.IO) { conn.connect(); conn.sendText(TodoDraft.prompt(item.text)) }
                            result.await()
                        }
                        val tasks=TodoDraft.parse(text)
                        if(tasks.isEmpty()) { state.text=t("메모에서 할 일을 찾지 못했습니다.", "タスクが見つかりませんでした。", "No actionable tasks found."); return@launch }
                        val input=EditText(this@LibraryActivity).apply { setText(tasks.joinToString("\n")); minLines=3; maxLines=7; filters=arrayOf(android.text.InputFilter.LengthFilter(8000)) }
                        val d=AlertDialog.Builder(this@LibraryActivity).setTitle(t("한 줄에 할 일 하나", "1行に1つのタスク", "One task per line"))
                            .setView(input).setNegativeButton(t("취소", "キャンセル", "Cancel"),null).setPositiveButton(t("목록 저장", "リスト保存", "Save list"),null).create()
                        d.setOnShowListener { d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val lines=input.text.toString().lines().map { it.trim() }.filter { it.isNotBlank() }.distinct()
                            if(lines.isEmpty() || lines.size>50 || lines.any { it.length>500 }) { input.error=t("50개 이하의 짧은 할 일을 입력하세요", "短いタスクを50個以内で入力", "Enter up to 50 short tasks"); return@setOnClickListener }
                            val todo=SavedItem(UUID.randomUUID().toString(),"todo",t("할 일: ", "タスク: ", "Tasks: ")+item.title,lines.joinToString("\n"),System.currentTimeMillis(),lines.map { TaskItem(it) })
                            commit(saved+todo) { mode=2; render(); d.dismiss() }
                        } }; dialog=d; d.show()
                    } catch(_:TimeoutCancellationException) { state.text=t("응답 시간이 초과되었습니다. 다시 시도해 주세요.", "応答がタイムアウトしました。", "Response timed out. Please retry.") }
                    catch(e:CancellationException) { throw e }
                    catch(_:Exception) { state.text=t("정리 실패. 원본 메모는 유지됩니다.", "整理できませんでした。元のメモは保持されます。", "Could not extract tasks. Original memo preserved.") }
                    finally { conn.close(); if(taskConnection===conn) taskConnection=null }
                }
            }.show()
    }

    private fun memoDialog() {
        if(!ready || writing || memoJob?.isActive==true || taskJob?.isActive==true || recorder!=null) return
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),7); return }
        val config=(application as MuseApp).store.elevenLabs()
        if(config==null) { Toast.makeText(this,t("ElevenLabs 등록이 필요합니다", "ElevenLabsの設定が必要です", "Configure ElevenLabs first"),Toast.LENGTH_LONG).show(); return }
        val d=AlertDialog.Builder(this).setTitle(t("음성 메모 · 최대 20초", "音声メモ · 最大20秒", "Voice memo · up to 20s"))
            .setMessage(t("녹음 후 ElevenLabs로 글자를 변환합니다. 확인 후 기기에 저장하며 Muse에는 보내지 않습니다.", "音声はElevenLabsで文字に変換します。確認後に端末へ保存し、Museには送りません。", "ElevenLabs transcribes the audio. Review and save text locally; nothing is sent to Muse."))
            .setNegativeButton(t("취소", "キャンセル", "Cancel"),null).setPositiveButton(t("녹음 시작", "録音開始", "Record"),null).create()
        fun finishCapture() {
            val active=recorder ?: return
            recorder=null
            val wav=active.finish()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            d.dismiss()
            if(wav==null) { Toast.makeText(this,t("녹음이 너무 짧습니다", "録音が短すぎます", "Recording too short"),Toast.LENGTH_SHORT).show(); return }
            state.text=t("음성 메모 변환 중…", "文字に変換中…", "Transcribing memo…")
            memoJob=scope.launch {
                try {
                    val transcript=ElevenLabsClient(config).transcribe(wav)
                    ensureActive()
                    editMemo(SavedItem(UUID.randomUUID().toString(),"memo","",transcript,System.currentTimeMillis()))
                } catch(e:CancellationException) { throw e }
                catch(_:Exception) { state.text=t("음성 변환 실패. 다시 녹음해 주세요.", "文字変換に失敗しました。", "Transcription failed. Record again.") }
            }
        }
        d.setOnShowListener { d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if(recorder!=null) finishCapture() else {
                val capture=VoiceRecorder({ runOnUiThread { finishCapture() } },{ runOnUiThread { recorder?.finish(); recorder=null; d.dismiss(); state.text=t("녹음 오류", "録音エラー", "Recording error") } })
                try { capture.start(); recorder=capture; window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); d.setMessage(t("● 녹음 중", "● 録音中", "● Recording")); d.getButton(AlertDialog.BUTTON_POSITIVE).text=t("종료·글자 변환", "停止・文字変換", "Stop & transcribe") }
                catch(_:Exception) { state.text=t("마이크를 열 수 없습니다", "マイクを開けません", "Microphone unavailable"); d.dismiss() }
            }
        } }
        d.setOnDismissListener { recorder?.finish(); recorder=null; window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        dialog=d; d.show()
    }
    override fun onPause() {
        recorder?.finish(); recorder=null; memoJob?.cancel(); taskJob?.cancel(); taskConnection?.close(); taskConnection=null; dialog?.dismiss()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
