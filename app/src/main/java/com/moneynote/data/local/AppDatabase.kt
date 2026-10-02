package com.moneynote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneynote.data.local.dao.BudgetDao
import com.moneynote.data.local.dao.CategoryDao
import com.moneynote.data.local.dao.TxnDao
import com.moneynote.data.local.entity.Budget
import com.moneynote.data.local.entity.Category
import com.moneynote.data.local.entity.Txn
import com.moneynote.log.AppLog

@Database(
    entities = [Txn::class, Category::class, Budget::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun txnDao(): TxnDao

    abstract fun categoryDao(): CategoryDao

    abstract fun budgetDao(): BudgetDao

    companion object {
        private const val DB_NAME = "moneynote.db"

        private const val TAG = "AppDatabase"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                // 首次创建库时在同一事务里写入默认分类，保证 UI 首次查询就能拿到数据
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        AppLog.info(TAG, "首次创建数据库，开始写入默认分类")
                        db.beginTransaction()
                        try {
                            SeedData.DEFAULT_CATEGORIES.forEach { seed ->
                                db.execSQL(
                                    "INSERT INTO categories " +
                                        "(name, emoji, type, sortOrder, isBuiltIn, archived) " +
                                        "VALUES (?, ?, ?, ?, 1, 0)",
                                    arrayOf<Any?>(
                                        seed.name,
                                        seed.emoji,
                                        seed.type.name,
                                        seed.sortOrder,
                                    ),
                                )
                            }
                            db.setTransactionSuccessful()
                        } catch (t: Throwable) {
                            AppLog.error(TAG, "写入默认分类失败，事务将回滚", t)
                            throw t
                        } finally {
                            db.endTransaction()
                        }
                        AppLog.info(
                            TAG,
                            "默认分类写入完成，共 ${SeedData.DEFAULT_CATEGORIES.size} 个",
                        )
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        AppLog.info(TAG, "数据库已打开：${db.path}，版本 ${db.version}")
                    }
                })
                .build()
    }
}
