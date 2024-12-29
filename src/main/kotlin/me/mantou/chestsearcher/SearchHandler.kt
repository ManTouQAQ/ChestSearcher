package me.mantou.chestsearcher

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import org.bukkit.Bukkit
import org.bukkit.block.Container
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.slf4j.LoggerFactory


class SearchHandler(
    private val player: Player,
    private val manager: SearchManager
) : ChannelInboundHandlerAdapter() {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val glowEntityIds = mutableListOf<SearchManager.SearchEntityItem>()

    fun addGoldEntities(entities: List<SearchManager.SearchEntityItem>) {
        glowEntityIds.addAll(entities)
    }

    fun removeEntities(entities: List<SearchManager.SearchEntityItem>) {
        glowEntityIds.removeAll(entities)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext?) {
        logger.debug("${player.name} SearchHandler注入成功")
        super.handlerAdded(ctx)
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext?) {
        logger.debug("${player.name} SearchHandler被移除")
        super.handlerRemoved(ctx)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ServerboundInteractPacket) {
            val ids = glowEntityIds.map { it.entity.id }
            takeIf {
                ids.contains(msg.entityId)
            }?.takeIf {
                !msg.isAttack && !msg.isUsingSecondaryAction
            }?.apply {
                glowEntityIds.find {
                    it.entity.id == msg.entityId
                }?.apply {
                    (player as CraftPlayer).swingMainHand()
                    Bukkit.getScheduler().scheduleSyncDelayedTask(manager.plugin) {
                        player.openInventory((this.loc.block.state as Container).inventory)
                    }
                    return
                }
            }
        }

        super.channelRead(ctx, msg)
    }
}