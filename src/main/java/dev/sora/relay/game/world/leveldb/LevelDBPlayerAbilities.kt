package dev.sora.relay.game.world.leveldb

import org.cloudburstmc.nbt.NbtMap

class LevelDBPlayerAbilities {
    var canAttackMobs = true
    var canAttackPlayers = true
    var canBuild = true
    var canFly = false
    var canInstaBuild = false // TODO: what exactly is this? How does it differ from canBuild?
    var canMine = true
    var canOpenContainers = true
    var canTeleport = false
    var canUseDoorsAndSwitches = true
    var flySpeed = 0.05f
    var isFlying = false
    var isInvulnerable = false
    var isOp = false
    var isLightning = false // TODO: what is this?
    var permissionsLevel = 0
    var playerPermissionsLevel = 0 // TODO: what is this and isn't it the same as permissionLevel?
    var walkSpeed = 0.1f

	fun toNbtMap(): NbtMap {
		return NbtMap.builder()
			.putBoolean("attackmobs", canAttackMobs)
			.putBoolean("attackplayers", canAttackPlayers)
			.putBoolean("build", canBuild)
			.putBoolean("mayfly", canFly)
			.putBoolean("instabuild", canInstaBuild)
			.putBoolean("mine", canMine)
			.putBoolean("opencontainers", canOpenContainers)
			.putBoolean("teleport", canTeleport)
			.putBoolean("doorsandswitches", canUseDoorsAndSwitches)
			.putFloat("flySpeed", flySpeed)
			.putBoolean("flying", isFlying)
			.putBoolean("invulnerable", isInvulnerable)
			.putBoolean("op", isOp)
			.putBoolean("lightning", isLightning)
			.putInt("permissionsLevel", permissionsLevel)
			.putInt("playerPermissionsLevel", playerPermissionsLevel)
			.putFloat("walkSpeed", walkSpeed)
			.build()
	}
}
