package me.mantou.chestsearcher

import net.kyori.adventure.text.Component
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Items
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.command.defaults.BukkitCommand
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.inventory.CraftItemType
import org.bukkit.entity.Player

class SearchCommand(
    name: String,
    private val manager: SearchManager
) : BukkitCommand(name) {

    companion object{
        const val MAX_RADIUS = 32
        const val DEFAULT_RADIUS = 16
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {

        if (sender !is Player){
            sender.sendMessage(Component.translatable("chestsearcher.command.search.not-player"))
            return false
        }
        val material: Material
        var radius = DEFAULT_RADIUS

        if (args.isEmpty()){
            val handle = (sender.inventory.itemInMainHand as CraftItemStack).handle
            material = CraftItemType.minecraftToBukkit(handle?.item ?: Items.AIR)
        }else {
            if (args[0] == "help"){
                sender.sendMessage(Component.translatable("chestsearcher.command.search.usage"))
                return true
            }

            if (args.size > 1){
                radius = args[1].toInt()
                if (radius > MAX_RADIUS) radius = MAX_RADIUS
            }

            val result = resLocToMaterial(args[0])
            if (result == null){
                sender.sendMessage(Component.translatable("chestsearcher.command.search.invalid-material"))
                return false
            }
            material = result
        }

        if (material.isAir) {
            sender.sendMessage(Component.translatable("chestsearcher.command.search.search-air"))
            return false
        }

        manager.searchAsync(sender, radius, material, {
            sender.sendMessage(
                Component.translatable(
                    "chestsearcher.command.search.response",
                    Component.text(radius),
                    Component.text(it.size),
                    Component.translatable(material.translationKey())
                )
            )
            it
        })
        return true
    }

    private fun resLocToMaterial(itemKey: String): Material? {
        val resourceLocation = try {
            ResourceLocation(itemKey)
        } catch (e: Exception) {
            return null
        }

        if (!BuiltInRegistries.ITEM.containsKey(resourceLocation)) {
            return null
        }

        val item = BuiltInRegistries.ITEM.get(resourceLocation)
        return CraftItemType.minecraftToBukkit(item)
    }
}