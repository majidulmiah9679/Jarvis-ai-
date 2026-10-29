package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessageEntity(
    val id: Long = 0,
    val role: String, // "user", "jarvis", "system"
    val text: String,
    val detailedText: String = "",
    val language: String = "en", // "en" or "bn"
    val actionName: String? = null,
    val actionStatus: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class MemoryEntity(
    val id: Long = 0,
    val category: String, // "PREFERENCE", "FACT", "CONTACT", "ROUTINE"
    val fact: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ActionLogEntity(
    val id: Long = 0,
    val toolName: String,
    val target: String,
    val parametersSummary: String,
    val status: String, // "EXECUTED", "CONFIRMED", "CANCELLED", "BLOCKED", "FAILED"
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface JarvisDao {
    fun getAllMessages(): Flow<List<ChatMessageEntity>>
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>
    suspend fun insertMessage(message: ChatMessageEntity): Long
    suspend fun clearAllMessages()

    fun getAllMemories(): Flow<List<MemoryEntity>>
    suspend fun getMemoriesSnapshot(): List<MemoryEntity>
    suspend fun insertMemory(memory: MemoryEntity): Long
    suspend fun deleteMemoryById(id: Long)
    suspend fun clearAllMemories()

    fun getAllActionLogs(): Flow<List<ActionLogEntity>>
    suspend fun insertActionLog(log: ActionLogEntity): Long
    suspend fun clearAllActionLogs()
}

class JarvisDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "jarvis_neural_core.db", null, 1),
    JarvisDao {

    private val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private val _memoriesFlow = MutableStateFlow<List<MemoryEntity>>(emptyList())
    private val _actionLogsFlow = MutableStateFlow<List<ActionLogEntity>>(emptyList())

    init {
        dbScope.launch {
            refreshMessages()
            refreshMemories()
            refreshActionLogs()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                role TEXT NOT NULL,
                text TEXT NOT NULL,
                detailedText TEXT NOT NULL,
                language TEXT NOT NULL,
                actionName TEXT,
                actionStatus TEXT,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                fact TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS action_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                toolName TEXT NOT NULL,
                target TEXT NOT NULL,
                parametersSummary TEXT NOT NULL,
                status TEXT NOT NULL,
                details TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS chat_messages")
        db.execSQL("DROP TABLE IF EXISTS saved_memories")
        db.execSQL("DROP TABLE IF EXISTS action_logs")
        onCreate(db)
    }

    fun jarvisDao(): JarvisDao = this

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = _messagesFlow.asStateFlow()
    override fun getAllMemories(): Flow<List<MemoryEntity>> = _memoriesFlow.asStateFlow()
    override fun getAllActionLogs(): Flow<List<ActionLogEntity>> = _actionLogsFlow.asStateFlow()

    override suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<ChatMessageEntity>()
            val db = readableDatabase
            db.rawQuery(
                "SELECT id, role, text, detailedText, language, actionName, actionStatus, timestamp FROM chat_messages ORDER BY timestamp DESC LIMIT ?",
                arrayOf(limit.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(
                        ChatMessageEntity(
                            id = cursor.getLong(0),
                            role = cursor.getString(1),
                            text = cursor.getString(2),
                            detailedText = cursor.getString(3),
                            language = cursor.getString(4),
                            actionName = if (cursor.isNull(5)) null else cursor.getString(5),
                            actionStatus = if (cursor.isNull(6)) null else cursor.getString(6),
                            timestamp = cursor.getLong(7)
                        )
                    )
                }
            }
            result
        }

    override suspend fun insertMessage(message: ChatMessageEntity): Long =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("role", message.role)
                put("text", message.text)
                put("detailedText", message.detailedText)
                put("language", message.language)
                put("actionName", message.actionName)
                put("actionStatus", message.actionStatus)
                put("timestamp", message.timestamp)
            }
            val rowId = writableDatabase.insert("chat_messages", null, values)
            refreshMessages()
            rowId
        }

    override suspend fun clearAllMessages() = withContext(Dispatchers.IO) {
        writableDatabase.delete("chat_messages", null, null)
        refreshMessages()
    }

    override suspend fun getMemoriesSnapshot(): List<MemoryEntity> =
        withContext(Dispatchers.IO) {
            queryAllMemoriesInternal()
        }

    override suspend fun insertMemory(memory: MemoryEntity): Long =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("category", memory.category)
                put("fact", memory.fact)
                put("timestamp", memory.timestamp)
            }
            val rowId = writableDatabase.insert("saved_memories", null, values)
            refreshMemories()
            rowId
        }

    override suspend fun deleteMemoryById(id: Long) = withContext(Dispatchers.IO) {
        writableDatabase.delete("saved_memories", "id = ?", arrayOf(id.toString()))
        refreshMemories()
    }

    override suspend fun clearAllMemories() = withContext(Dispatchers.IO) {
        writableDatabase.delete("saved_memories", null, null)
        refreshMemories()
    }

    override suspend fun insertActionLog(log: ActionLogEntity): Long =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put("toolName", log.toolName)
                put("target", log.target)
                put("parametersSummary", log.parametersSummary)
                put("status", log.status)
                put("details", log.details)
                put("timestamp", log.timestamp)
            }
            val rowId = writableDatabase.insert("action_logs", null, values)
            refreshActionLogs()
            rowId
        }

    override suspend fun clearAllActionLogs() = withContext(Dispatchers.IO) {
        writableDatabase.delete("action_logs", null, null)
        refreshActionLogs()
    }

    private fun refreshMessages() {
        val list = mutableListOf<ChatMessageEntity>()
        readableDatabase.rawQuery(
            "SELECT id, role, text, detailedText, language, actionName, actionStatus, timestamp FROM chat_messages ORDER BY timestamp ASC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    ChatMessageEntity(
                        id = cursor.getLong(0),
                        role = cursor.getString(1),
                        text = cursor.getString(2),
                        detailedText = cursor.getString(3),
                        language = cursor.getString(4),
                        actionName = if (cursor.isNull(5)) null else cursor.getString(5),
                        actionStatus = if (cursor.isNull(6)) null else cursor.getString(6),
                        timestamp = cursor.getLong(7)
                    )
                )
            }
        }
        _messagesFlow.value = list
    }

    private fun queryAllMemoriesInternal(): List<MemoryEntity> {
        val list = mutableListOf<MemoryEntity>()
        readableDatabase.rawQuery(
            "SELECT id, category, fact, timestamp FROM saved_memories ORDER BY timestamp DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    MemoryEntity(
                        id = cursor.getLong(0),
                        category = cursor.getString(1),
                        fact = cursor.getString(2),
                        timestamp = cursor.getLong(3)
                    )
                )
            }
        }
        return list
    }

    private fun refreshMemories() {
        _memoriesFlow.value = queryAllMemoriesInternal()
    }

    private fun refreshActionLogs() {
        val list = mutableListOf<ActionLogEntity>()
        readableDatabase.rawQuery(
            "SELECT id, toolName, target, parametersSummary, status, details, timestamp FROM action_logs ORDER BY timestamp DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    ActionLogEntity(
                        id = cursor.getLong(0),
                        toolName = cursor.getString(1),
                        target = cursor.getString(2),
                        parametersSummary = cursor.getString(3),
                        status = cursor.getString(4),
                        details = cursor.getString(5),
                        timestamp = cursor.getLong(6)
                    )
                )
            }
        }
        _actionLogsFlow.value = list
    }

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = JarvisDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

class JarvisRepository(private val dao: JarvisDao) {
    val allMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessages()
    val allMemories: Flow<List<MemoryEntity>> = dao.getAllMemories()
    val allActionLogs: Flow<List<ActionLogEntity>> = dao.getAllActionLogs()

    suspend fun getRecentMessages(limit: Int = 14): List<ChatMessageEntity> =
        dao.getRecentMessages(limit).reversed()

    suspend fun getMemoriesSnapshot(): List<MemoryEntity> = dao.getMemoriesSnapshot()

    suspend fun addMessage(message: ChatMessageEntity): Long = dao.insertMessage(message)

    suspend fun clearConversationHistory() = dao.clearAllMessages()

    suspend fun addMemory(category: String, fact: String): Long =
        dao.insertMemory(MemoryEntity(category = category, fact = fact))

    suspend fun deleteMemory(id: Long) = dao.deleteMemoryById(id)

    suspend fun clearMemories() = dao.clearAllMemories()

    suspend fun logAction(
        toolName: String,
        target: String,
        parametersSummary: String,
        status: String,
        details: String
    ): Long = dao.insertActionLog(
        ActionLogEntity(
            toolName = toolName,
            target = target,
            parametersSummary = parametersSummary,
            status = status,
            details = details
        )
    )

    suspend fun clearActionLogs() = dao.clearAllActionLogs()

    suspend fun wipeAllData() {
        dao.clearAllMessages()
        dao.clearAllMemories()
        dao.clearAllActionLogs()
    }
}
