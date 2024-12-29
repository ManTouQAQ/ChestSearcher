package me.mantou.chestsearcher

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.slf4j.LoggerFactory
import java.util.*

class SearchManager(val plugin: Plugin): Listener {
    companion object{
        const val SEARCH_HANDLER_IDENTITY = "search_handler"
        const val PACKET_HANDLER_IDENTITY = "packet_handler"
    }

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val userSearchHandlerStore = mutableMapOf<UUID, SearchHandler>()

    fun init() {
        Bukkit.getPluginManager().registerEvents(this, plugin)

        Bukkit.getOnlinePlayers().forEach {
            add(it)
        }
    }

    fun destroy(){
        Bukkit.getOnlinePlayers().forEach {
            remove(it)
        }
    }

    fun removePacketEntities(
        player: Player,
        entity: List<SearchEntityItem>
    ) {
        val packet = ClientboundRemoveEntitiesPacket(
            *entity.map { it.entity.id }.toIntArray()
        )
        player.sendPacket(packet)

        userSearchHandlerStore[player.uniqueId]!!.removeEntities(entity)
    }

    fun sendPacketGlowShulkers(
        target: Player,
        loc: List<Location>
    ): List<SearchEntityItem> {
        val store = mutableListOf<SearchEntityItem>()
        loc.forEach {
            val shulker = EntityType.SHULKER.create((it.world as CraftWorld).handle)

            val packet = ClientboundAddEntityPacket(
                shulker!!,
                0,
                BlockPos(it.blockX, it.blockY, it.blockZ)
            )

            target.sendPacket(packet)

            //set glowing
            val dataValue = DataValue.create(
                EntityDataAccessor(0, EntityDataSerializers.BYTE),
                (0x20 or 0x40).toByte()
            )

            val dataPacket = ClientboundSetEntityDataPacket(
                shulker.id,
                listOf(dataValue)
            )
            target.sendPacket(dataPacket)

            store.add(SearchEntityItem(shulker, it))
        }

        userSearchHandlerStore[target.uniqueId]!!.addGoldEntities(store)
        return store
    }

    fun add(player: Player){
        val uuid = player.uniqueId

        if (userSearchHandlerStore.contains(uuid)){
            logger.error("${player.name} 已经被添加到SearchManager了, 尝试添加失败")
            return
        }

        val pipeline = (player as CraftPlayer)
            .handle
            .connection
            .connection
            .channel
            .pipeline()

        val handler = SearchHandler(player, this)

        pipeline.addBefore(
            PACKET_HANDLER_IDENTITY,
            SEARCH_HANDLER_IDENTITY,
            handler
        )

        userSearchHandlerStore[uuid] = handler

        logger.debug("${player.name} 成功添加到SearchManager")
    }

    fun remove(player: Player){
        val uuid = player.uniqueId
        if (userSearchHandlerStore.contains(uuid).not()){
            logger.error("${player.name} 没有被添加到SearchManager, 尝试移除失败")
            return
        }

        val pipeline = (player as CraftPlayer)
            .handle
            .connection
            .connection
            .channel
            .pipeline()

        if (pipeline[SEARCH_HANDLER_IDENTITY] != null){
            pipeline.remove(SEARCH_HANDLER_IDENTITY)
        }

        userSearchHandlerStore.remove(uuid)
        logger.debug("${player.name} 从SearchManager中移除")
    }

    fun searchAsync(
        player: Player,
        radius: Int,
        material: Material,
        onSearchFilter: (List<Location>) -> List<Location> = { it },
        tick: Long = 5 * 20L
    ) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val locs = searchContainerLocs(player.location, radius, material)

            val entities = sendPacketGlowShulkers(
                player,
                onSearchFilter.invoke(locs)
            )

            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
                removePacketEntities(player, entities)
            }, tick)
        })
    }

    fun searchContainerLocs(
        centerLoc: Location,
        radius: Int,
        type: Material
    ): List<Location> {
        return Bukkit.getScheduler().callSyncMethod(plugin) {
            val store: MutableList<Location> = ArrayList()

            for (x in centerLoc.blockX - radius..centerLoc.blockX + radius) {
                for (y in centerLoc.blockY - radius..centerLoc.blockY + radius) {
                    for (z in centerLoc.blockZ - radius..centerLoc.blockZ + radius) {
                        val atLoc = Location(
                            centerLoc.world,
                            x.toDouble(),
                            y.toDouble(),
                            z.toDouble()
                        )
                        atLoc.block.state.takeIf {
                            it is Container
                        }?.let {
                            it as Container
                        }?.takeIf {
                            it.inventory.contains(type)
                        }?.apply {
                            store.add(atLoc)
                        }
                    }
                }
            }

            store
        }.get()
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent){
        add(e.player)
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent){
        remove(e.player)
    }

    data class SearchEntityItem(
        val entity: Entity,
        val loc: Location
    )
}

fun Player.sendPacket(packet: Packet<*>){
    (this as CraftPlayer).handle.connection.sendPacket(packet)
}