package dev.sora.relay.cheat.module.impl

import com.nukkitx.protocol.bedrock.data.SoundEvent
import com.nukkitx.protocol.bedrock.packet.LevelSoundEvent2Packet
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.Listen
import dev.sora.relay.game.event.impl.EventTick
import dev.sora.relay.game.utils.NoteBlockUtils
import java.io.File

class ModuleBGM : CheatModule("BGM") {

    val song = NoteBlockUtils.Song().apply {
        readNbs(File("test.nbs").inputStream())
        computeMaxTicks()
    }
    var ticks = 0

    override fun onEnable() {
        ticks = -1
        chat("${song.maxTicks / 20} seconds")
    }

    @Listen
    fun onTick(event: EventTick) {
        if (ticks + 20 > song.maxTicks) {
            ticks = 0
        }
        val note = song.ticks[ticks++] ?: return
        note.forEach { note ->
            val pk = LevelSoundEvent2Packet().apply {
                sound = SoundEvent.NOTE
                position = event.session.thePlayer.vec3Position()
                extraData = note.inst.ordinal shl 8 or note.key.toInt()
                identifier = ":"
                isBabySound = false
                isRelativeVolumeDisabled = false
            }
            event.session.netSession.outboundPacket(pk)
        }
    }
}