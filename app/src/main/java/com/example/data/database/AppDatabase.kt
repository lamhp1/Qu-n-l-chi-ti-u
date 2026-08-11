package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BudgetDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.TransactionDao
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [Transaction::class, Category::class, Budget::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quan_ly_chi_tieu_db"
                )
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                // Populate default categories
                database.categoryDao().insertCategories(Category.DEFAULT_EXPENSE_CATEGORIES)
                database.categoryDao().insertCategories(Category.DEFAULT_INCOME_CATEGORIES)

                // Populate sample realistic transactions for initial launch display
                val now = Calendar.getInstance()
                val currentMonthStr = String.format("%02d/%d", now.get(Calendar.MONTH) + 1, now.get(Calendar.YEAR))

                // Default budget for current month
                database.budgetDao().insertOrUpdateBudget(
                    Budget(
                        categoryName = "ALL",
                        monthlyLimit = 15000000.0,
                        monthYear = currentMonthStr
                    )
                )
                database.budgetDao().insertOrUpdateBudget(
                    Budget(
                        categoryName = "Ăn uống",
                        monthlyLimit = 5000000.0,
                        monthYear = currentMonthStr
                    )
                )
                database.budgetDao().insertOrUpdateBudget(
                    Budget(
                        categoryName = "Mua sắm",
                        monthlyLimit = 3000000.0,
                        monthYear = currentMonthStr
                    )
                )

                // Sample transactions
                val today = System.currentTimeMillis()
                val dayMs = 86400000L

                val sampleTransactions = listOf(
                    Transaction(
                        title = "Lương hàng tháng",
                        amount = 18500000.0,
                        type = TransactionType.INCOME,
                        categoryName = "Lương",
                        categoryIcon = "payments",
                        categoryColorHex = "#2E7D32",
                        note = "Lương công ty Chuyển khoản",
                        dateMillis = today - dayMs * 3
                    ),
                    Transaction(
                        title = "Ăn sáng Phở Bò",
                        amount = 55000.0,
                        type = TransactionType.EXPENSE,
                        categoryName = "Ăn uống",
                        categoryIcon = "restaurant",
                        categoryColorHex = "#FB8C00",
                        note = "Tái nạm trứng chần",
                        dateMillis = today
                    ),
                    Transaction(
                        title = "Cà phê Highlands",
                        amount = 49000.0,
                        type = TransactionType.EXPENSE,
                        categoryName = "Ăn uống",
                        categoryIcon = "restaurant",
                        categoryColorHex = "#FB8C00",
                        note = "Phin sữa đá",
                        dateMillis = today
                    ),
                    Transaction(
                        title = "Mua sắm siêu thị Go!",
                        amount = 850000.0,
                        type = TransactionType.EXPENSE,
                        categoryName = "Mua sắm",
                        categoryIcon = "shopping_bag",
                        categoryColorHex = "#E91E63",
                        note = "Đồ dùng gia đình tuần này",
                        dateMillis = today - dayMs * 1
                    ),
                    Transaction(
                        title = "Đổ xăng xe máy",
                        amount = 90000.0,
                        type = TransactionType.EXPENSE,
                        categoryName = "Di chuyển",
                        categoryIcon = "directions_car",
                        categoryColorHex = "#1E88E5",
                        note = "Xăng RON 95",
                        dateMillis = today - dayMs * 2
                    ),
                    Transaction(
                        title = "Thưởng hoàn thành dự án",
                        amount = 2500000.0,
                        type = TransactionType.INCOME,
                        categoryName = "Thưởng",
                        categoryIcon = "card_giftcard",
                        categoryColorHex = "#FFB300",
                        note = "Bonus Quý 3",
                        dateMillis = today - dayMs * 2
                    ),
                    Transaction(
                        title = "Tiền internet & Điện thoại",
                        amount = 350000.0,
                        type = TransactionType.EXPENSE,
                        categoryName = "Hóa đơn & Tiện ích",
                        categoryIcon = "receipt_long",
                        categoryColorHex = "#3949AB",
                        note = "Gói cước tháng 8",
                        dateMillis = today - dayMs * 4
                    )
                )

                database.transactionDao().insertTransactions(sampleTransactions)
            }
        }
    }
}
