package dev.sora.relay.game.utils

import java.io.DataInputStream
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
            val dataInputStream = DataInputStream(inputIn)
            var length = readShort(dataInputStream)
            var firstcustominstrument = 10 //Backward compatibility - most of songs with old structure are from 1.12
            var nbsversion = 0
            if (length.toInt() == 0) {
                nbsversion = dataInputStream.readByte().toInt()
                firstcustominstrument = dataInputStream.readByte().toInt()
                if (nbsversion >= 3) {
                    length = readShort(dataInputStream)
                }
            }
            readShort(dataInputStream)
            readString(dataInputStream)
            readString(dataInputStream)
            readString(dataInputStream) // original author
            readString(dataInputStream)
            val speed = readShort(dataInputStream) / 100f
            dataInputStream.readBoolean() // auto-save
            dataInputStream.readByte() // auto-save duration
            dataInputStream.readByte() // x/4ths, time signature
            readInt(dataInputStream) // minutes spent on project
            readInt(dataInputStream) // left clicks (why?)
            readInt(dataInputStream) // right clicks (why?)
            readInt(dataInputStream) // blocks added
            readInt(dataInputStream) // blocks removed
            readString(dataInputStream) // .mid/.schematic file name
            if (nbsversion >= 4) {
                dataInputStream.readByte() // loop on/off
                dataInputStream.readByte() // max loop count
                readShort(dataInputStream) // loop start tick
            }
            var tick: Short = -1
            while (true) {
                val jumpTicks = readShort(dataInputStream) // jumps till next tick
                if (jumpTicks.toInt() == 0) {
                    break
                }
                tick = (tick + jumpTicks).toShort()
                while (true) {
                    val jumpLayers = readShort(dataInputStream) // jumps till next layer
                    if (jumpLayers.toInt() == 0) {
                        break
                    }
                    val instrument: Byte = dataInputStream.readByte()
                    val key: Byte = (dataInputStream.readByte() - 33).toByte()
                    if (nbsversion >= 4) {
                        dataInputStream.readByte() // note block velocity
                        dataInputStream.readByte() // note block panning
                        readShort(dataInputStream) // note block pitch
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

    private fun readShort(dataInputStream: DataInputStream): Short {
        val byte1 = dataInputStream.readUnsignedByte()
        val byte2 = dataInputStream.readUnsignedByte()
        return (byte1 + (byte2 shl 8)).toShort()
    }

    private fun readInt(dataInputStream: DataInputStream): Int {
        val byte1 = dataInputStream.readUnsignedByte()
        val byte2 = dataInputStream.readUnsignedByte()
        val byte3 = dataInputStream.readUnsignedByte()
        val byte4 = dataInputStream.readUnsignedByte()
        return byte1 + (byte2 shl 8) + (byte3 shl 16) + (byte4 shl 24)
    }

    private fun readString(dataInputStream: DataInputStream): String {
        var length = readInt(dataInputStream)
        val builder = StringBuilder(length)
        while (length > 0) {
            var c = Char(dataInputStream.readByte().toUShort())
            if (c == 0x0D.toChar()) {
                c = ' '
            }
            builder.append(c)
            --length
        }
        return builder.toString()
    }
}