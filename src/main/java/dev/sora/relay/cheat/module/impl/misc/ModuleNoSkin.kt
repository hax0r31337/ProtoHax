package dev.sora.relay.cheat.module.impl.misc

import dev.sora.relay.cheat.module.CheatCategory
import dev.sora.relay.cheat.module.CheatModule
import dev.sora.relay.game.event.EventPacketInbound
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket

class ModuleNoSkin : CheatModule("NoSkin", CheatCategory.MISC) {

    private val skinData by lazy {
        ImageData.of(ByteArray(16384).also { for(i in it.indices) it[i] = Byte.MAX_VALUE })
    }

	private val handlePacketInbound = handle<EventPacketInbound> {
		if (packet is PlayerListPacket) {
			packet.entries.forEach {
				if (it.skin != null) {
					it.skin = generateSkin(it.skin)
				}
			}
		} else if (packet is PlayerSkinPacket && packet.skin != null) {
			packet.skin = generateSkin(packet.skin)
		}
	}

    private fun generateSkin(skin: SerializedSkin): SerializedSkin {
        return SerializedSkin.of(skin.skinId, skin.playFabId, SKIN_RESOURCE_PATCH, skinData,
            emptyList(), ImageData.EMPTY, SKIN_GEOMETRY_DATA, "0.0.0", "",
            false, true, false, false, "", skin.fullSkinId,
            "wide", "#0", emptyList(), emptyList()
        )
    }

	private companion object {
        const val SKIN_GEOMETRY_DATA = "{\"format_version\":\"1.12.0\",\"minecraft:geometry\":[{\"bones\":[{\"name\":\"body\",\"parent\":\"waist\",\"pivot\":[0,24,0]},{\"name\":\"waist\",\"pivot\":[0,12,0]},{\"cubes\":[{\"origin\":[-5,8,3],\"size\":[10,16,1],\"uv\":[0,0]}],\"name\":\"cape\",\"parent\":\"body\",\"pivot\":[0,24,3],\"rotation\":[0,180,0]}],\"description\":{\"identifier\":\"geometry.cape\",\"texture_height\":32,\"texture_width\":64}},{\"bones\":[{\"name\":\"root\",\"pivot\":[0,0,0]},{\"cubes\":[{\"origin\":[-4,12,-2],\"size\":[8,12,4],\"uv\":[16,16]}],\"name\":\"body\",\"parent\":\"waist\",\"pivot\":[0,24,0]},{\"name\":\"waist\",\"parent\":\"root\",\"pivot\":[0,12,0]},{\"cubes\":[{\"origin\":[-4,24,-4],\"size\":[8,8,8],\"uv\":[0,0]}],\"name\":\"head\",\"parent\":\"body\",\"pivot\":[0,24,0]},{\"name\":\"cape\",\"parent\":\"body\",\"pivot\":[0,24,3]},{\"cubes\":[{\"inflate\":0.5,\"origin\":[-4,24,-4],\"size\":[8,8,8],\"uv\":[32,0]}],\"name\":\"hat\",\"parent\":\"head\",\"pivot\":[0,24,0]},{\"cubes\":[{\"origin\":[4,12,-2],\"size\":[4,12,4],\"uv\":[32,48]}],\"name\":\"leftArm\",\"parent\":\"body\",\"pivot\":[5,22,0]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[4,12,-2],\"size\":[4,12,4],\"uv\":[48,48]}],\"name\":\"leftSleeve\",\"parent\":\"leftArm\",\"pivot\":[5,22,0]},{\"name\":\"leftItem\",\"parent\":\"leftArm\",\"pivot\":[6,15,1]},{\"cubes\":[{\"origin\":[-8,12,-2],\"size\":[4,12,4],\"uv\":[40,16]}],\"name\":\"rightArm\",\"parent\":\"body\",\"pivot\":[-5,22,0]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[-8,12,-2],\"size\":[4,12,4],\"uv\":[40,32]}],\"name\":\"rightSleeve\",\"parent\":\"rightArm\",\"pivot\":[-5,22,0]},{\"locators\":{\"lead_hold\":[-6,15,1]},\"name\":\"rightItem\",\"parent\":\"rightArm\",\"pivot\":[-6,15,1]},{\"cubes\":[{\"origin\":[-0.1,0,-2],\"size\":[4,12,4],\"uv\":[16,48]}],\"name\":\"leftLeg\",\"parent\":\"root\",\"pivot\":[1.9,12,0]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[-0.1,0,-2],\"size\":[4,12,4],\"uv\":[0,48]}],\"name\":\"leftPants\",\"parent\":\"leftLeg\",\"pivot\":[1.9,12,0]},{\"cubes\":[{\"origin\":[-3.9,0,-2],\"size\":[4,12,4],\"uv\":[0,16]}],\"name\":\"rightLeg\",\"parent\":\"root\",\"pivot\":[-1.9,12,0]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[-3.9,0,-2],\"size\":[4,12,4],\"uv\":[0,32]}],\"name\":\"rightPants\",\"parent\":\"rightLeg\",\"pivot\":[-1.9,12,0]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[-4,12,-2],\"size\":[8,12,4],\"uv\":[16,32]}],\"name\":\"jacket\",\"parent\":\"body\",\"pivot\":[0,24,0]}],\"description\":{\"identifier\":\"geometry.humanoid.custom\",\"texture_height\":64,\"texture_width\":64,\"visible_bounds_height\":2,\"visible_bounds_offset\":[0,1,0],\"visible_bounds_width\":1}},{\"bones\":[{\"name\":\"root\",\"pivot\":[0,0,0]},{\"name\":\"waist\",\"parent\":\"root\",\"pivot\":[0,12,0]},{\"cubes\":[{\"origin\":[-4,12,-2],\"size\":[8,12,4],\"uv\":[16,16]}],\"name\":\"body\",\"parent\":\"waist\",\"pivot\":[0,24,0]},{\"cubes\":[{\"origin\":[-4,24,-4],\"size\":[8,8,8],\"uv\":[0,0]}],\"name\":\"head\",\"parent\":\"body\",\"pivot\":[0,24,0]},{\"cubes\":[{\"inflate\":0.5,\"origin\":[-4,24,-4],\"size\":[8,8,8],\"uv\":[32,0]}],\"name\":\"hat\",\"parent\":\"head\",\"pivot\":[0,24,0]},{\"cubes\":[{\"origin\":[-3.9,0,-2],\"size\":[4,12,4],\"uv\":[0,16]}],\"name\":\"rightLeg\",\"parent\":\"root\",\"pivot\":[-1.9,12,0]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[-3.9,0,-2],\"size\":[4,12,4],\"uv\":[0,32]}],\"name\":\"rightPants\",\"parent\":\"rightLeg\",\"pivot\":[-1.9,12,0]},{\"cubes\":[{\"origin\":[-0.1,0,-2],\"size\":[4,12,4],\"uv\":[16,48]}],\"mirror\":true,\"name\":\"leftLeg\",\"parent\":\"root\",\"pivot\":[1.9,12,0]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[-0.1,0,-2],\"size\":[4,12,4],\"uv\":[0,48]}],\"name\":\"leftPants\",\"parent\":\"leftLeg\",\"pivot\":[1.9,12,0]},{\"cubes\":[{\"origin\":[4,11.5,-2],\"size\":[3,12,4],\"uv\":[32,48]}],\"name\":\"leftArm\",\"parent\":\"body\",\"pivot\":[5,21.5,0]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[4,11.5,-2],\"size\":[3,12,4],\"uv\":[48,48]}],\"name\":\"leftSleeve\",\"parent\":\"leftArm\",\"pivot\":[5,21.5,0]},{\"name\":\"leftItem\",\"parent\":\"leftArm\",\"pivot\":[6,14.5,1]},{\"cubes\":[{\"origin\":[-7,11.5,-2],\"size\":[3,12,4],\"uv\":[40,16]}],\"name\":\"rightArm\",\"parent\":\"body\",\"pivot\":[-5,21.5,0]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[-7,11.5,-2],\"size\":[3,12,4],\"uv\":[40,32]}],\"name\":\"rightSleeve\",\"parent\":\"rightArm\",\"pivot\":[-5,21.5,0]},{\"locators\":{\"lead_hold\":[-6,14.5,1]},\"name\":\"rightItem\",\"parent\":\"rightArm\",\"pivot\":[-6,14.5,1]},{\"cubes\":[{\"inflate\":0.25,\"origin\":[-4,12,-2],\"size\":[8,12,4],\"uv\":[16,32]}],\"name\":\"jacket\",\"parent\":\"body\",\"pivot\":[0,24,0]},{\"name\":\"cape\",\"parent\":\"body\",\"pivot\":[0,24,-3]}],\"description\":{\"identifier\":\"geometry.humanoid.customSlim\",\"texture_height\":64,\"texture_width\":64,\"visible_bounds_height\":2,\"visible_bounds_offset\":[0,1,0],\"visible_bounds_width\":1}}]}"
        const val SKIN_RESOURCE_PATCH = "{\"geometry\":{\"default\":\"geometry.humanoid.custom\"}}"
    }
}
