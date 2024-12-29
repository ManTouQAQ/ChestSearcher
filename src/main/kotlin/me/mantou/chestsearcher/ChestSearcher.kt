package me.mantou.chestsearcher

import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationRegistry
import net.kyori.adventure.util.UTF8ResourceBundleControl
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.*

class ChestSearcher : JavaPlugin() {
    private val manager = SearchManager(this)
    private lateinit var translationRegistry: TranslationRegistry
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object{
        val LANG_LIST = listOf(
            Locale.of("zh", "CN"),
            Locale.of("en", "US"),
        )

        val DEFAULT_LANG = Locale.of("en", "US")
    }

    override fun onEnable() {
        manager.init()

        translationRegistry = TranslationRegistry.create(Key.key("chestsearcher:lang"))
        for (locale in LANG_LIST) {
            val bundle = ResourceBundle.getBundle("lang.CS", locale, UTF8ResourceBundleControl.get())
            translationRegistry.registerAll(locale, bundle, true)
        }
        translationRegistry.defaultLocale(DEFAULT_LANG)
        GlobalTranslator.translator().addSource(translationRegistry)

        Bukkit.getCommandMap().register(
            "chestsearcher",
            SearchCommand("search", manager)
        )
    }

    override fun onDisable() {
        manager.destroy()
        GlobalTranslator.translator().removeSource(translationRegistry)
    }
}