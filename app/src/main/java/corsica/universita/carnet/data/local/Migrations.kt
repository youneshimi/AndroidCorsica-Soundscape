package corsica.universita.carnet.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sounds ADD COLUMN profileId TEXT")
        db.execSQL("ALTER TABLE sounds ADD COLUMN nickname TEXT")
        db.execSQL("ALTER TABLE sounds ADD COLUMN likesCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sounds ADD COLUMN listensCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS profiles (id TEXT NOT NULL PRIMARY KEY, nickname TEXT NOT NULL, avatarUrl TEXT, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS likes (soundId TEXT NOT NULL PRIMARY KEY, liked INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS comments (id TEXT NOT NULL PRIMARY KEY, soundId TEXT NOT NULL, profileId TEXT NOT NULL, nickname TEXT NOT NULL, text TEXT NOT NULL, createdAt TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_comments_soundId ON comments (soundId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS trails (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL, profileId TEXT NOT NULL, nickname TEXT NOT NULL, createdAt TEXT NOT NULL, soundsCount INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS trail_sounds (trailId TEXT NOT NULL, soundId TEXT NOT NULL, position INTEGER NOT NULL, note TEXT NOT NULL, PRIMARY KEY(trailId, soundId))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_trail_sounds_trailId ON trail_sounds (trailId)")
    }
}
