package com.payas.quelle.sync

enum class SyncAction { Upload, Download, None }

/** Yerel ve buluttaki verinin hangisi daha yeniyse o kazanır. */
fun decideSync(localUpdatedAt: Long, remoteUpdatedAt: Long?): SyncAction = when {
    remoteUpdatedAt == null -> if (localUpdatedAt > 0) SyncAction.Upload else SyncAction.None
    remoteUpdatedAt > localUpdatedAt -> SyncAction.Download
    localUpdatedAt > remoteUpdatedAt -> SyncAction.Upload
    else -> SyncAction.None
}
