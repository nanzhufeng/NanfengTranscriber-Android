package com.nanzhufeng.transcriber.data.invocation

import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationRecord
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAsrInvocationRepository(
    private val dao: AsrInvocationDao,
) : AsrInvocationRecorder {
    override suspend fun record(record: AsrInvocationRecord) {
        dao.insert(record.toEntity())
    }

    fun observeAll(): Flow<List<AsrInvocationRecord>> = dao.observeAll().map { records ->
        records.map(AsrInvocationRecordEntity::toDomain)
    }
}
