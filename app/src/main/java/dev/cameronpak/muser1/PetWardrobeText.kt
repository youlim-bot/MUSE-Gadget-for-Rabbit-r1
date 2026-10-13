package dev.cameronpak.muser1

import android.content.Context

internal object PetWardrobeText {
    fun title(c:Context)=UiText.text(c,"옷장","クローゼット","Wardrobe")
    fun slot(c:Context,s:PetWardrobeSlot)=when(s){
        PetWardrobeSlot.HEAD->UiText.text(c,"머리","頭","Head")
        PetWardrobeSlot.NECK->UiText.text(c,"목","首","Neck")
        PetWardrobeSlot.BODY->UiText.text(c,"옷","服","Body")
        PetWardrobeSlot.FACE->UiText.text(c,"안경","眼鏡","Face")
    }
    fun name(c:Context,id:String)=when(id){
        "beanie"->UiText.text(c,"포근한 비니","ほっこりニット帽","Cozy beanie")
        "flowers"->UiText.text(c,"꽃 왕관","花かんむり","Flower crown")
        "bow"->UiText.text(c,"파란 나비넥타이","青い蝶ネクタイ","Blue bow tie")
        "scarf"->UiText.text(c,"빨간 목도리","赤いマフラー","Red scarf")
        "raincoat"->UiText.text(c,"노란 우비","黄色いレインコート","Yellow raincoat")
        "sweater"->UiText.text(c,"줄무늬 스웨터","しましまセーター","Striped sweater")
        "glasses"->UiText.text(c,"동그란 안경","まるい眼鏡","Round glasses")
        else->UiText.text(c,"선글라스","サングラス","Sunglasses")
    }
    fun hint(c:Context,id:String)=when(id){
        "beanie"->UiText.text(c,"알에서 태어나면 만나요","生まれたら会えるよ","Earned when Muse hatches")
        "flowers"->UiText.text(c,"어린 뮤즈로 자라면 만나요","こどもMuseに育つと会えるよ","Earned as a young Muse")
        "bow"->UiText.text(c,"함께 첫 식사를 해요","初めてのごはんを一緒に","Share the first meal")
        "scarf"->UiText.text(c,"태어난 뒤 뽀송하게 씻겨 주세요","生まれたらお風呂でさっぱり","Give Muse a bath after hatching")
        "raincoat"->UiText.text(c,"별잡기·리듬·낚시에서 성공해요","星・リズム・釣りで成功しよう","Win star catch, rhythm or fishing")
        "sweater"->UiText.text(c,"자라는 뮤즈가 되면 만나요","育ちざかりMuseになると会えるよ","Earned as a growing Muse")
        "glasses"->UiText.text(c,"반짝임 기억하기에서 성공해요","光を覚えるゲームで成功しよう","Win memory lights")
        else->UiText.text(c,"어른 뮤즈로 자라면 만나요","おとなMuseに育つと会えるよ","Earned as an adult Muse")
    }
    fun notice(c:Context,ids:List<String>)=UiText.text(c,"함께 돌본 선물이 옷장에 도착했어요","お世話の贈りものがクローゼットに届いたよ","A little care reward arrived in your wardrobe")+" · "+ids.joinToString(" · "){name(c,it)}
}
