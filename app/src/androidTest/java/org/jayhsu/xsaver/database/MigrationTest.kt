package org.jayhsu.xsaver.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.jayhsu.xsaver.data.database.AppDatabase
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version DB then migrate through to latest.
        helper.createDatabase(TEST_DB, 1).apply { close() }
        // TODO: Add inserted rows for validation per version.
        // migrate to latest (5)
        helper.runMigrationsAndValidate(TEST_DB, 5, true).apply {
            // 简单验证新列存在（通过查询失败即抛异常）
            query("PRAGMA table_info('download_tasks')").use { c ->
                var hasErrorType = false
                var hasErrorCode = false
                while (c.moveToNext()) {
                    val name = c.getString(c.getColumnIndexOrThrow("name"))
                    if (name == "errorType") hasErrorType = true
                    if (name == "errorCode") hasErrorCode = true
                }
                assertTrue(hasErrorType)
                assertTrue(hasErrorCode)
            }
        }
    }
}