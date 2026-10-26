package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CardProgressEntity
import com.example.data.model.GroupProgressEntity
import com.example.data.model.QuizAttemptEntity

@Database(
  entities = [
    CardProgressEntity::class,
    GroupProgressEntity::class,
    QuizAttemptEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class ExamWiseDatabase : RoomDatabase() {

  abstract fun cardProgressDao(): CardProgressDao
  abstract fun groupProgressDao(): GroupProgressDao
  abstract fun quizAttemptDao(): QuizAttemptDao

  companion object {
    @Volatile
    private var INSTANCE: ExamWiseDatabase? = null

    fun getDatabase(context: Context): ExamWiseDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          ExamWiseDatabase::class.java,
          "examwise_database"
        ).fallbackToDestructiveMigration()
         .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
