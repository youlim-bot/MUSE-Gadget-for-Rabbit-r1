package dev.cameronpak.muser1

import android.content.Context

internal object PetActivityText {
    private fun t(c:Context,k:String,j:String,e:String)=UiText.text(c,k,j,e)
    fun place(c:Context,i:Int)=when(i){0->t(c,"속삭이는 숲","ささやきの森","Whispering forest");1->t(c,"조개 해변","貝がらの浜","Seashell beach");else->t(c,"별빛 언덕","星あかりの丘","Starlight hill")}
    fun item(c:Context,i:Int)=when(i){0->t(c,"잎사귀 모빌","葉っぱのモビール","Leaf mobile");1->t(c,"조개 장식","貝がらの飾り","Seashell ornament");2->t(c,"별빛 램프","星あかりランプ","Star lamp");3->t(c,"작은 화분","小さな植木鉢","Little planter");4->t(c,"무지개 액자","虹の額縁","Rainbow frame");5->t(c,"달빛 쿠션","月あかりクッション","Moon cushion");else->t(c,"장식 없음","飾りなし","No decoration")}
    fun target(c:Context,i:Int)=when(i){0->t(c,"초록색 물건 찾기","緑のものを探そう","Find something green");1->t(c,"둥근 물건 찾기","丸いものを探そう","Find something round");else->t(c,"꽃 찾기","花を探そう","Find a flower")}
    fun promise(c:Context,i:Int)=when(i){0->t(c,"한국어로 이야기하기","韓国語でおしゃべり","Korean conversation");1->t(c,"일본어로 이야기하기","日本語でおしゃべり","Japanese conversation");2->t(c,"영어로 이야기하기","英語でおしゃべり","English conversation");else->t(c,"하루 이야기 나누기","一日の話をしよう","Share our day")}
    fun event(c:Context,e:PetEvent):String=when(e.kind){
        "welcome"->t(c,"너와 함께하는 기록을 시작했어.","きみとの記録が始まったよ。","Our story began today.")
        "hatch"->t(c,"알에서 나와 너를 만났어!","たまごから出て、きみに会えた！","I hatched and met you!")
        "care"->t(c,"네가 다정하게 돌봐 줬어.","やさしくお世話してくれたよ。","You took gentle care of me.")
        "food"->t(c,"너와 맛있는 식사를 했어.","いっしょにおいしいごはんを食べたよ。","We enjoyed a meal together.")
        "play"->t(c,"너와 함께 놀아서 즐거웠어.","いっしょに遊んで楽しかったよ。","Playing together made me happy.")
        "sleep"->t(c,"포근하게 쉬었어.","ゆっくり休んだよ。","I settled down for a rest.")
        "chat"->t(c,"너와 이야기를 나눴어.","きみとおしゃべりしたよ。","We had a conversation.")
        "depart"->t(c,"${place(c,e.value.toIntOrNull()?:0)}에 탐험을 떠났어.","${place(c,e.value.toIntOrNull()?:0)}へ探検に出たよ。","I set off for ${place(c,e.value.toIntOrNull()?:0)}.")
        "recall"->t(c,"일찍 집으로 돌아왔어. 다음에 또 가자.","早めに帰ったよ。また今度ね。","I came home early. There's always another day.")
        "trip"->{val parts=e.value.split('|');val p=parts.getOrNull(0)?.toIntOrNull()?:0;val item=parts.getOrNull(1)?.toIntOrNull()?:-1
            if(item<0)t(c,"남겨진 탐험 기록을 소중히 보관했어.","探検の記録を大切に残したよ。","The expedition record was preserved.")
            else { val detail=when(parts.getOrNull(2)){"CURIOUS"->t(c,"낯선 길이 궁금했어.","知らない道が気になったよ。","I was curious about unfamiliar paths.");"SHY","GRUMPY"->t(c,"조용한 곳에서 잠시 쉬었어.","静かな場所でひと休みしたよ。","I paused in a quiet spot.");"PLAYFUL"->t(c,"신나게 주변을 둘러봤어.","わくわくしながら見て回ったよ。","I explored with excitement.");else->t(c,"네 생각을 하며 걸었어.","きみのことを思いながら歩いたよ。","I thought of you along the way.")}
                t(c,"${place(c,p)}에서 ${item(c,item)}을 가져왔어. $detail","${place(c,p)}から${item(c,item)}を持ち帰ったよ。$detail","I brought back a ${item(c,item)} from ${place(c,p)}. $detail") }
        }
        "mission"->t(c,"함께 ${target(c,e.value.toIntOrNull()?:0)}에 성공했어!","いっしょに「${target(c,e.value.toIntOrNull()?:0)}」を達成！","We completed: ${target(c,e.value.toIntOrNull()?:0)}!")
        "decorate"->t(c,"방의 장식을 바꿨어. 내 공간이 더 특별해졌어.","お部屋の飾りを変えたよ。もっと特別な場所になったね。","We changed the room. It feels more like ours.")
        "promise"->t(c,"${promise(c,e.value.toIntOrNull()?:3)} 약속을 함께 시작했어.","「${promise(c,e.value.toIntOrNull()?:3)}」の時間をいっしょに始めたよ。","We started our promise: ${promise(c,e.value.toIntOrNull()?:3)}.")
        "memory"->e.value
        else->""
    }
}
