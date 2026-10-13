package dev.cameronpak.muser1

import android.content.res.Resources
import android.graphics.*

/** Only the current stage is decoded; alpha margins do not change character placement. */
internal class PetArt(private val resources:Resources) {
    private data class Art(val resource:Int,val eyes:List<RectF>)
    internal data class Sprite(val bitmap:Bitmap,val eyes:List<RectF>,val skin:Int)
    private val stages=listOf(
        Art(R.drawable.pet_egg,emptyList()),
        Art(R.drawable.pet_baby,listOf(RectF(0.34076f,0.46178f,0.40764f,0.52866f),RectF(0.59236f,0.4586f,0.65924f,0.52866f))),
        Art(R.drawable.pet_child,listOf(RectF(0.37081f,0.46651f,0.42185f,0.51994f),RectF(0.57416f,0.46651f,0.62520f,0.51994f))),
        Art(R.drawable.pet_teen,listOf(RectF(0.39952f,0.40351f,0.43461f,0.44019f),RectF(0.55981f,0.40271f,0.59569f,0.43939f))),
        Art(R.drawable.pet_adult,listOf(RectF(0.36624f,0.36943f,0.40764f,0.41401f),RectF(0.56688f,0.36624f,0.60828f,0.41083f))),
        Art(R.drawable.pet_elder,listOf(RectF(0.36523f,0.42663f,0.42823f,0.44817f),RectF(0.56778f,0.42663f,0.63238f,0.44737f)))
    )
    private var stage=-1
    private var cached:Sprite?=null
    fun sprite(level:Int):Sprite {
        val index=level.coerceIn(0,stages.lastIndex)
        cached?.takeIf {stage==index}?.let {return it}
        val art=stages[index]
        val raw=BitmapFactory.decodeResource(resources,art.resource,BitmapFactory.Options().apply {inSampleSize=if(index==4)2 else 4})
        var left=raw.width;var top=raw.height;var right=0;var bottom=0
        for(y in 0 until raw.height)for(x in 0 until raw.width)if(Color.alpha(raw.getPixel(x,y))>128){
            left=minOf(left,x);top=minOf(top,y);right=maxOf(right,x+1);bottom=maxOf(bottom,y+1)
        }
        check(right>left && bottom>top)
        val bitmap=Bitmap.createBitmap(raw,left,top,right-left,bottom-top)
        val eyes=art.eyes.map {RectF((it.left*raw.width-left)/bitmap.width,(it.top*raw.height-top)/bitmap.height,
            (it.right*raw.width-left)/bitmap.width,(it.bottom*raw.height-top)/bitmap.height)}
        val skin=art.eyes.firstOrNull()?.let {eye->raw.getPixel((eye.centerX()*raw.width).toInt(),((eye.top-.025f)*raw.height).toInt())} ?: Color.WHITE
        if(bitmap!==raw)raw.recycle()
        cached?.bitmap?.recycle();stage=index
        return Sprite(bitmap,eyes,skin).also {cached=it}
    }
}
