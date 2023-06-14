package dev.sora.relay.game.utils

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.io.InputStream
import kotlin.math.floor


object NoteBlockUtils {

    class Song {
        val ticks = mutableMapOf<Int, MutableList<Note>>()
        var maxTicks = 0
            private set

        fun computeMaxTicks() {
            var maxTicks = 0
            ticks.keys.forEach {
                if (it > maxTicks) {
                    maxTicks = it
                }
            }
            this.maxTicks = maxTicks
        }

        fun readNbs(inputIn: InputStream) {
			val buf = Unpooled.wrappedBuffer(inputIn.readBytes())
            val length = buf.readShortLE()
//            var firstcustominstrument = 10 // Backward compatibility - most of songs with old structure are from 1.12
            var nbsversion = 0
            if (length.toInt() == 0) {
                nbsversion = buf.readUnsignedByte().toInt()
//                firstcustominstrument =
					buf.readUnsignedByte().toInt()
                if (nbsversion >= 3) {
//                    length =
						buf.readShortLE()
                }
            }
            buf.readShortLE()
			buf.readString()
			buf.readString()
			buf.readString() // original author
			buf.readString()
            val speed = buf.readShortLE() / 100f
            buf.readBoolean() // auto-save
            buf.readByte() // auto-save duration
            buf.readByte() // x/4ths, time signature
            buf.readIntLE() // minutes spent on project
            buf.readIntLE() // left clicks (why?)
            buf.readIntLE() // right clicks (why?)
            buf.readIntLE() // blocks added
            buf.readIntLE() // blocks removed
            buf.readString() // .mid/.schematic file name
            if (nbsversion >= 4) {
                buf.readByte() // loop on/off
                buf.readByte() // max loop count
                buf.readShortLE() // loop start tick
            }
            var tick: Short = -1
            while (true) {
                val jumpTicks = buf.readShortLE() // jumps till next tick
                if (jumpTicks.toInt() == 0) {
                    break
                }
                tick = (tick + jumpTicks).toShort()
                while (true) {
                    val jumpLayers = buf.readShortLE() // jumps till next layer
                    if (jumpLayers.toInt() == 0) {
                        break
                    }
                    val instrument = buf.readUnsignedByte()
                    val key = (buf.readUnsignedByte() - 33).toByte()
                    if (nbsversion >= 4) {
                        buf.readByte() // note block velocity
                        buf.readByte() // note block panning
                        buf.readShortLE() // note block pitch
                    }
                    val realTick = floor(tick * 20f / speed).toInt()
                    (ticks[realTick] ?: mutableListOf<Note>().also { ticks[realTick] = it })
                        .add(Note(Instrument.values()[instrument.toInt()], key))
                }
            }
        }
    }

    data class Note(val inst: Instrument, val key: Byte)

    enum class Instrument {
        PIANO,
        BASS_DRUM,
        DRUM,
        STICKS,
        BASS,
        GLOCKENSPIEL,
        FLUTE,
        CHIME,
        GUITAR,
        XYLOPHONE,
        VIBRAPHONE,
        COW_BELL,
        DIDGERIDOO,
        SQUARE_WAVE,
        BANJO,
        ELECTRIC_PIANO;
    }

    private fun ByteBuf.readString(): String {
        var length = readIntLE()
        val builder = StringBuilder(length)
        while (length > 0) {
            var c = Char(readByte().toUShort())
            if (c == 0x0D.toChar()) {
                c = ' '
            }
            builder.append(c)
            --length
        }
        return builder.toString()
    }
}
