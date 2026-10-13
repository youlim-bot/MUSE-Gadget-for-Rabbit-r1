package dev.cameronpak.muser1

import android.content.Context

internal object PetWorldText {
    private fun t(c:Context,k:String,j:String,e:String)=UiText.text(c,k,j,e)
    fun seed(c:Context,i:Int)=when(i){0->t(c,"숲의 씨앗","森の種","Forest seed");1->t(c,"바닷바람 씨앗","潮風の種","Sea-breeze seed");else->t(c,"별빛 씨앗","星あかりの種","Starlight seed")}
    fun flower(c:Context,i:Int)=when(i){0->t(c,"햇살꽃","ひだまりの花","Sunshine flower");1->t(c,"노을꽃","夕焼けの花","Sunset flower");2->t(c,"달빛꽃","月あかりの花","Moonlight flower");else->t(c,"이슬꽃","しずくの花","Dewdrop flower")}
    fun skill(c:Context,i:Int)=when(i){0->t(c,"반갑게 인사하기","元気にあいさつ","Friendly greeting");1->t(c,"기쁨의 춤","よろこびのダンス","Happy dance");else->t(c,"신나는 점프","わくわくジャンプ","Happy jump")}
    fun discovery(c:Context,i:Int)=when(i){0->t(c,"식물","植物","Plants");1->t(c,"물건","もの","Objects");else->t(c,"풍경","景色","Scenes")}
    fun photoPrompt(c:Context,i:Int)=t(c,"발견 도감에 남길 사진이야. 눈에 보이는 ${discovery(c,i)}을 한국어로 짧게 설명하고, 확실하지 않은 이름은 추측하지 말아 줘.","発見図鑑に残す写真です。写っている${discovery(c,i)}を日本語で短く説明して。不確かな名前は推測しないでね。","This photo is for our discovery album. Briefly describe the visible ${discovery(c,i)} in English. Do not guess an uncertain name.")
    fun chapter(c:Context,i:Int,route:Int):String {
        val path=if(route==0)t(c,"따뜻한 숲길","あたたかな森の道","the welcoming forest path")else t(c,"낯선 별빛 길","未知の星あかりの道","the unfamiliar starlight path")
        return when(i){
            0->t(c,"숲 입구에 작은 봉투가 놓여 있었어. 안에는 지도가 한 장 들어 있어. 우리는 어느 길로 가 볼까?","森の入口に小さな封筒。中には一枚の地図があったよ。どの道へ行こう？","At the forest gate we found a small envelope with a map. Which path shall we take?")
            1->t(c,"$path 을 걷다가 비를 만났어. 작은 새가 나뭇잎 아래에서 떨고 있어. 어떻게 할까?","$path を歩いていると雨。小鳥が葉っぱの下で震えているよ。どうしよう？","Walking along $path, we met the rain. A small bird shivered beneath a leaf. What shall we do?")
            2->t(c,"앞에 반짝이는 개울이 있어. 오래된 다리와 징검다리가 보이네. 뮤즈가 네 선택을 기다리고 있어.","きらきらした小川に着いたよ。古い橋と飛び石がある。Museはきみの選択を待っているよ。","A sparkling stream lies ahead. There is an old bridge and a line of stepping stones. Muse waits for your choice.")
            3->t(c,"길가에서 바구니를 발견했어. 누군가 떨어뜨린 것 같아. 멀리 작은 발자국도 보여.","道ばたにかごを見つけたよ。誰かの落とし物かな。遠くに小さな足あともある。","We found a basket by the trail. Someone may have dropped it. Small footprints lead away.")
            4->t(c,"해가 지고 반딧불이들이 나타났어. 길을 밝히는 빛도 있고, 깊은 숲으로 향하는 빛도 있어.","日が暮れると蛍が現れたよ。道を照らす光と、森の奥へ向かう光がある。","At dusk the fireflies appeared. Some light the path; others wander into the deep forest.")
            5->t(c,"언덕에서 조용한 음악이 들려. 뮤즈는 흥얼거리고 싶기도, 가만히 듣고 싶기도 해.","丘から静かな音楽。Museは歌いたいような、そっと聴きたいような気持ち。","Soft music drifts down the hill. Muse feels like humming along, or simply listening.")
            else->t(c,"드디어 언덕 꼭대기에 도착했어. 동이 트기 시작해. 함께한 여행을 어떤 마음으로 기억할까?","ついに丘の頂上。空が明るくなってきたよ。この旅をどんな気持ちで覚えておこう？","We reached the hilltop as dawn broke. How shall we remember our journey?")
        }
    }
    fun choice(c:Context,i:Int,option:Int):String = if(option==0)when(i){
        0->t(c,"친구들이 사는 숲길","友だちの住む森へ","Visit the forest friends")
        1->t(c,"새에게 우산을 만들어 주기","小鳥に傘を作る","Make a little umbrella")
        2->t(c,"다리를 천천히 건너기","橋をゆっくり渡る","Cross the bridge carefully")
        3->t(c,"주인을 기다리기","持ち主を待つ","Wait for the owner")
        4->t(c,"밝은 길을 따라가기","明るい道を歩く","Follow the lit path")
        5->t(c,"조용히 함께 듣기","いっしょに静かに聴く","Listen together quietly")
        else->t(c,"만난 친구들에게 고마워하기","出会った友だちに感謝","Thank our new friends")
    }else when(i){
        0->t(c,"지도에 없는 길 탐험하기","地図にない道へ","Explore the unmarked path")
        1->t(c,"새와 함께 쉼터 찾기","小鳥と雨宿りを探す","Find shelter with the bird")
        2->t(c,"징검다리를 건너기","飛び石を渡る","Try the stepping stones")
        3->t(c,"발자국을 따라가 보기","足あとをたどる","Follow the footprints")
        4->t(c,"반딧불이와 새 길 찾기","蛍と新しい道を探す","Find a new trail with the fireflies")
        5->t(c,"뮤즈와 노래 부르기","Museと歌う","Sing along with Muse")
        else->t(c,"다음 여행을 약속하기","次の旅を約束","Promise another adventure")
    }
    fun outcome(c:Context,e:StoryChoice):String = if(e.choice==0)when(e.chapter){
        0->t(c,"숲 친구들이 우리에게 따뜻한 차를 나눠 줬어. 뮤즈는 조용히 웃었어.","森の友だちがお茶を分けてくれたよ。Museはそっと笑った。","The forest friends shared warm tea. Muse smiled softly.")
        1->t(c,"큰 잎으로 만든 우산 아래에서 새가 안심했어. 친구가 한 명 늘었네.","大きな葉の傘に小鳥も安心。友だちがひとり増えたね。","The bird relaxed beneath our leaf umbrella. We made a friend.")
        2->t(c,"서로 손을 잡고 다리를 건넜어. 개울 소리를 오래 기억할 것 같아.","手をつないで橋を渡ったよ。小川の音を忘れない。","We crossed hand in hand. We will remember the sound of the stream.")
        3->t(c,"다람쥐가 돌아와 바구니를 찾아갔어. 기다려 줘서 고맙다고 했어.","戻ってきたリスがかごを見つけたよ。待ってくれてありがとうって。","A squirrel returned for the basket and thanked us for waiting.")
        4->t(c,"반딧불이들이 안전한 길을 밝혀 줬어. 작은 빛도 큰 도움이 되네.","蛍が道を照らしてくれたよ。小さな光も頼もしいね。","The fireflies lit our way. Even a little light can help.")
        5->t(c,"풀밭에 앉아 음악을 들었어. 말없이도 편안한 순간이었어.","草の上で音楽を聴いたよ。言葉がなくても心地よかった。","We sat in the grass and listened. We felt comfortable without words.")
        else->t(c,"아침 햇살 속에서 친구들에게 손을 흔들었어. 고마운 마음을 집으로 가져가자.","朝日に友だちへ手を振ったよ。ありがとうの気持ちを持ち帰ろう。","We waved to our friends in the sunrise. Let's bring our gratitude home.")
    }else when(e.chapter){
        0->t(c,"새로운 길에서 반짝이는 돌을 발견했어. 뮤즈의 눈도 반짝였어.","新しい道できらきらの石を発見。Museの目も輝いたよ。","On the new path we found a sparkling stone. Muse's eyes sparkled too.")
        1->t(c,"빈 나무 구멍에서 함께 비를 피했어. 새가 숨겨진 길을 알려 줬어.","木の洞で雨宿り。小鳥が秘密の道を教えてくれたよ。","We sheltered in a hollow tree. The bird showed us a secret path.")
        2->t(c,"돌을 하나씩 건너다 물고기를 만났어. 뮤즈와 함께 손을 흔들었어.","石を渡っていると魚が見えたよ。Museと手を振った。","Hopping across the stones, we spotted fish and waved together.")
        3->t(c,"발자국 끝에서 다람쥐를 만났어. 바구니를 돌려주니 작은 지도를 선물했어.","足あとの先にリス。かごを返すと小さな地図をくれたよ。","The footprints led to a squirrel. Returning its basket earned us a little map.")
        4->t(c,"반딧불이를 따라 숨겨진 꽃밭을 발견했어. 다음에 또 오고 싶어.","蛍を追うと秘密の花畑。いつかまた来たいね。","Following the fireflies, we discovered a hidden flower meadow.")
        5->t(c,"우리 노래에 새들이 화음을 더했어. 뮤즈가 신나게 몸을 흔들었어.","ふたりの歌に小鳥がハーモニー。Museも楽しそうに揺れたよ。","The birds joined our song. Muse swayed happily.")
        else->t(c,"아침 하늘을 보며 다음 여행을 약속했어. 모르는 길도 함께라면 즐거울 거야.","朝空に次の旅を約束。知らない道もふたりなら楽しいね。","Under the morning sky we promised another trip. Unfamiliar paths are fun together.")
    }
    fun ending(c:Context,route:Int)=if(route==0)t(c,"우리 이야기: 마음을 밝히는 등불\n천천히 걷고 친구를 기다리며, 서로를 더 잘 알게 되었어.","ふたりの物語：心を照らす灯り\nゆっくり歩き、友だちを待ちながら、互いをもっと知ったよ。","Our story: A light for the heart\nWalking slowly and waiting for friends brought us closer.")else t(c,"우리 이야기: 반짝이는 나침반\n새로운 길을 함께 선택하며, 다음 모험이 기다려지게 되었어.","ふたりの物語：きらめくコンパス\n新しい道を選びながら、次の冒険が楽しみになったよ。","Our story: The sparkling compass\nChoosing new paths together made us look forward to another adventure.")
}
