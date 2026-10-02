package com.moneynote.data.repository

import com.moneynote.data.local.AppDatabase
import com.moneynote.data.local.entity.Budget
import com.moneynote.data.local.entity.Category
import com.moneynote.data.local.entity.Txn
import com.moneynote.data.model.TxnType
import com.moneynote.log.AppLog
import com.moneynote.util.toEpochMillis
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * 唯一的数据入口。UI 层只依赖这里，不直接接触 DAO 与 Room 实体以外的细节。
 */
class LedgerRepository(db: AppDatabase) {

    private val txnDao = db.txnDao()
    private val categoryDao = db.categoryDao()
    private val budgetDao = db.budgetDao()

    // ---------------------------------------------------------------- 流水

    fun observeAllTxns(): Flow<List<Txn>> = txnDao.observeAll()

    fun observeTxnsBetween(startInclusive: LocalDate, endInclusive: LocalDate): Flow<List<Txn>> =
        txnDao.observeBetween(
            startInclusive.toEpochMillis(),
            endInclusive.plusDays(1).toEpochMillis(),
        )

    fun observeTxnsOfMonth(month: YearMonth): Flow<List<Txn>> =
        observeTxnsBetween(month.atDay(1), month.atEndOfMonth())

    suspend fun findTxn(id: Long): Txn? = txnDao.findById(id)

    suspend fun saveTxn(txn: Txn): Long {
        val isNew = txn.id == 0L
        val id = if (isNew) {
            txnDao.insert(txn)
        } else {
            txnDao.update(txn)
            txn.id
        }
        // 刻意不记录金额与备注：日志可能被用户导出分享，只留结构性信息足够定位问题
        AppLog.info(
            TAG,
            "保存流水：${if (isNew) "新增" else "更新"} id=$id，" +
                "类型=${txn.type}，分类=${txn.categoryId}",
        )
        return id
    }

    suspend fun deleteTxn(txn: Txn) {
        txnDao.delete(txn)
        AppLog.info(TAG, "删除流水 id=${txn.id}")
    }

    suspend fun deleteTxn(id: Long) {
        txnDao.deleteById(id)
        AppLog.info(TAG, "删除流水 id=$id")
    }

    // ---------------------------------------------------------------- 分类

    fun observeCategories(): Flow<List<Category>> = categoryDao.observeActive()

    fun observeCategories(type: TxnType): Flow<List<Category>> = categoryDao.observeByType(type)

    fun observeCategory(id: Long): Flow<Category?> = categoryDao.observeById(id)

    suspend fun findCategory(id: Long): Category? = categoryDao.findById(id)

    suspend fun countCategories(): Int = categoryDao.count()

    suspend fun countTxnsInCategory(categoryId: Long): Int = txnDao.countByCategory(categoryId)

    suspend fun nextCategorySortOrder(type: TxnType): Int = categoryDao.nextSortOrder(type)

    suspend fun saveCategory(category: Category): Long {
        val isNew = category.id == 0L
        val id = if (isNew) {
            categoryDao.insert(category)
        } else {
            categoryDao.update(category)
            category.id
        }
        AppLog.info(
            TAG,
            "保存分类：${if (isNew) "新增" else "更新"} id=$id，" +
                "「${category.name}」类型=${category.type}",
        )
        return id
    }

    /**
     * 分类被流水引用时不做物理删除，改为归档，防止历史统计口径被破坏。
     * 返回 true 表示执行的是归档。
     */
    suspend fun deleteOrArchiveCategory(category: Category, referenced: Boolean): Boolean {
        val archived = if (referenced) {
            categoryDao.archiveById(category.id)
            true
        } else {
            categoryDao.delete(category)
            false
        }
        AppLog.info(
            TAG,
            "分类 id=${category.id}「${category.name}」" +
                if (archived) "已被流水引用，改为归档" else "无流水引用，直接删除",
        )
        return archived
    }

    // ---------------------------------------------------------------- 预算

    fun observeBudgets(month: YearMonth): Flow<List<Budget>> =
        budgetDao.observeByMonth(month.toString())

    /** 金额 <= 0 视为清除该预算。 */
    suspend fun setBudget(month: YearMonth, categoryId: Long, amountCents: Long) {
        val key = month.toString()
        val existing = budgetDao.find(key, categoryId)
        val action = when {
            amountCents <= 0L -> {
                if (existing != null) budgetDao.delete(key, categoryId)
                "清除"
            }
            existing == null -> {
                budgetDao.insert(
                    Budget(yearMonth = key, categoryId = categoryId, amountCents = amountCents)
                )
                "新增"
            }
            else -> {
                budgetDao.update(existing.copy(amountCents = amountCents))
                "更新"
            }
        }
        AppLog.info(TAG, "预算$action：月份=$key，分类=$categoryId")
    }

    // ---------------------------------------------------------------- 其他

    suspend fun clearAllData() {
        txnDao.deleteAll()
        budgetDao.deleteAll()
        AppLog.warn(TAG, "已清空全部流水与预算设置（分类保留）")
    }

    private companion object {
        const val TAG = "LedgerRepository"
    }
}
