package com.example.questflow.data.database

import com.example.questflow.R
import com.example.questflow.data.database.dao.MemeDao
import com.example.questflow.data.database.entity.MemeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemeDataInitializer @Inject constructor(
    private val memeDao: MemeDao
) {
    suspend fun initializeMemes() = withContext(Dispatchers.IO) {
        // Check if memes already exist
        val existingMemes = memeDao.getAllMemes()
        if (existingMemes.isNotEmpty()) return@withContext

        // Insert initial memes
        val memes = listOf(
            MemeEntity(
                id = 1,
                name = "Success Kid",
                description = "Du hast es geschafft!",
                imageResourceId = R.drawable.meme_success_kid,
                rarity = "COMMON",
                requiredLevel = 2
            ),
            MemeEntity(
                id = 2,
                name = "Stonks",
                description = "Dein XP geht nur nach oben! 📈",
                imageResourceId = R.drawable.meme_stonks,
                rarity = "COMMON",
                requiredLevel = 3
            ),
            MemeEntity(
                id = 3,
                name = "This is Fine",
                description = "Wenn alles brennt, aber du trotzdem weitermachst",
                imageResourceId = R.drawable.meme_this_is_fine,
                rarity = "RARE",
                requiredLevel = 5
            ),
            MemeEntity(
                id = 4,
                name = "Galaxy Brain",
                description = "Deine Weisheit kennt keine Grenzen!",
                imageResourceId = R.drawable.meme_galaxy_brain,
                rarity = "EPIC",
                requiredLevel = 7
            ),
            MemeEntity(
                id = 5,
                name = "Drake Approves",
                description = "Die richtige Wahl getroffen!",
                imageResourceId = R.drawable.meme_drake,
                rarity = "RARE",
                requiredLevel = 10
            ),
            // Wiederholungen mit verschiedenen Seltenheiten für mehr Inhalte
            MemeEntity(
                id = 6,
                name = "Pro Success Kid",
                description = "Legendärer Erfolg!",
                imageResourceId = R.drawable.meme_success_kid,
                rarity = "LEGENDARY",
                requiredLevel = 15
            ),
            MemeEntity(
                id = 7,
                name = "Mega Stonks",
                description = "Zum Mond! 🚀",
                imageResourceId = R.drawable.meme_stonks,
                rarity = "EPIC",
                requiredLevel = 12
            ),
            MemeEntity(
                id = 8,
                name = "Everything is Fine",
                description = "Meister der Gelassenheit",
                imageResourceId = R.drawable.meme_this_is_fine,
                rarity = "LEGENDARY",
                requiredLevel = 20
            ),
            MemeEntity(
                id = 9,
                name = "Ascended Brain",
                description = "Transzendentale Intelligenz",
                imageResourceId = R.drawable.meme_galaxy_brain,
                rarity = "LEGENDARY",
                requiredLevel = 25
            ),
            MemeEntity(
                id = 10,
                name = "Drake Master",
                description = "Entscheidungsmeister",
                imageResourceId = R.drawable.meme_drake,
                rarity = "EPIC",
                requiredLevel = 18
            )
        )

        memes.forEach { meme ->
            memeDao.insertMeme(meme)
        }
    }
}