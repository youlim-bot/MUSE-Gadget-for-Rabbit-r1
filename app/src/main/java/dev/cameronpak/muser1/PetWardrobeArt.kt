package dev.cameronpak.muser1

import android.content.res.Resources
import android.graphics.*

/** The base PNG is read-only. Body overlays are separate, stage-shaped transparent resources. */
internal class PetWardrobeArt(private val resources:Resources) {
    private val cache=mutableMapOf<Int,Bitmap>()
    private val mesh=FloatArray(13*13*2)
    private fun bitmap(id:Int)=cache.getOrPut(id){BitmapFactory.decodeResource(resources,id)}
    fun draw(c:Canvas,s:PetState,sprite:PetArt.Sprite,w:Float,h:Float,baseMesh:FloatArray,brush:Paint,ms:Long,animated:Boolean,alpha:Float){
        val stage=s.level.coerceIn(1,5)
        val neckY=floatArrayOf(0f,.67f,.68f,.55f,.55f,.59f)[stage]
        val headWidth=floatArrayOf(0f,.66f,.74f,.70f,.70f,.51f)[stage]
        val headBottom=floatArrayOf(0f,.26f,.27f,.27f,.26f,.27f)[stage]
        fun accessory(id:Int,r:RectF){
            var p=0
            for(y in 0..12)for(x in 0..12){val px=r.left+x/12f*r.width();val py=r.top+y/12f*r.height();val v=py/h
                val sway=if(animated)kotlin.math.sin(ms/230.0+v*3).toFloat()*h*.009f*(v-.2f).coerceAtLeast(0f)else 0f
                mesh[p++]=px+sway;mesh[p++]=py
            }
            c.drawBitmapMesh(bitmap(id),12,12,mesh,0,null,0,brush)
        }
        brush.color=Color.WHITE;brush.alpha=(alpha*255).toInt()
        // Clothes first, then neck, head and face. All share the base's transform and sway function.
        PetWardrobe.visible(s).sortedBy {when(it.slot){PetWardrobeSlot.BODY->0;PetWardrobeSlot.NECK->1;PetWardrobeSlot.HEAD->2;PetWardrobeSlot.FACE->3}}.forEach {item->
            when(item.slot){
                PetWardrobeSlot.BODY->{
                    val ids=if(item.id=="sweater")intArrayOf(R.drawable.wardrobe_sweater_baby,R.drawable.wardrobe_sweater_child,R.drawable.wardrobe_sweater_teen,R.drawable.wardrobe_sweater_adult,R.drawable.wardrobe_sweater_elder)
                        else intArrayOf(R.drawable.wardrobe_raincoat_baby,R.drawable.wardrobe_raincoat_child,R.drawable.wardrobe_raincoat_teen,R.drawable.wardrobe_raincoat_adult,R.drawable.wardrobe_raincoat_elder)
                    c.drawBitmapMesh(bitmap(ids[stage-1]),12,12,baseMesh,0,null,0,brush)
                }
                PetWardrobeSlot.HEAD->{val id=if(item.id=="beanie")R.drawable.wardrobe_beanie else R.drawable.wardrobe_flowers;val b=bitmap(id);val aw=w*headWidth;val ah=aw*b.height/b.width;accessory(id,RectF((w-aw)/2,h*headBottom-ah,(w+aw)/2,h*headBottom))}
                PetWardrobeSlot.NECK->{val id=if(item.id=="bow")R.drawable.wardrobe_bow else R.drawable.wardrobe_scarf;val b=bitmap(id);val aw=w*if(item.id=="bow").28f else .53f;val ah=aw*b.height/b.width;accessory(id,RectF((w-aw)/2,h*neckY-ah*.22f,(w+aw)/2,h*neckY+ah*.78f))}
                PetWardrobeSlot.FACE->{if(sprite.eyes.size==2){val id=if(item.id=="glasses")R.drawable.wardrobe_glasses else R.drawable.wardrobe_sunglasses;val b=bitmap(id);val eyes=sprite.eyes;val x=(eyes[0].centerX()+eyes[1].centerX())*.5f*w;val y=(eyes[0].centerY()+eyes[1].centerY())*.5f*h;val aw=(eyes[1].centerX()-eyes[0].centerX())*w*2.08f;val ah=aw*b.height/b.width;accessory(id,RectF(x-aw/2,y-ah/2,x+aw/2,y+ah/2))}}
            }
        }
        brush.alpha=255
    }
}
