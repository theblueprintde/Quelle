package com.payas.quelle.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.payas.quelle.data.AppData
import com.payas.quelle.data.QuelleStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class SyncStatus(val label: String) {
    Off("Giriş yapılmadı"),
    Syncing("Eşitleniyor"),
    Synced("Eşitlendi"),
    Error("Eşitlenemedi, internet gelince tekrar denenecek"),
}

/**
 * Google ile giriş yapıldıysa tüm veriyi Firestore'da users/{uid}/data/main belgesinde tutar.
 * Yerelde her değişiklik kısa bir beklemeden sonra yüklenir; başka cihazdan gelen daha yeni veri
 * anında alınır. İnternet yoksa Firestore değişiklikleri sıraya koyar.
 */
class SyncManager(private val store: QuelleStore) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _user = MutableStateFlow(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _status = MutableStateFlow(SyncStatus.Off)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private var listener: ListenerRegistration? = null
    private var uploadJob: Job? = null
    private var attachedUid: String? = null

    @Volatile
    private var lastSynced = 0L

    fun start() {
        auth.addAuthStateListener { a ->
            _user.value = a.currentUser
            attach(a.currentUser)
        }
    }

    @OptIn(FlowPreview::class)
    private fun attach(user: FirebaseUser?) {
        if (user?.uid == attachedUid) return
        attachedUid = user?.uid
        listener?.remove()
        uploadJob?.cancel()
        if (user == null) {
            _status.value = SyncStatus.Off
            return
        }
        _status.value = SyncStatus.Syncing
        val doc = db.collection("users").document(user.uid).collection("data").document("main")

        listener = doc.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "Dinleme hatası", error)
                _status.value = SyncStatus.Error
                return@addSnapshotListener
            }
            val remoteAt = snap?.getLong("updatedAt")
            val text = snap?.getString("json")
            val local = store.data.value
            when (decideSync(local.updatedAt, remoteAt)) {
                SyncAction.Download -> {
                    val remote = text?.let { runCatching { store.decode(it) }.getOrNull() }
                    if (remote != null) {
                        lastSynced = remote.updatedAt
                        store.replaceFromRemote(remote)
                    }
                }
                SyncAction.Upload -> upload(doc, local)
                SyncAction.None -> lastSynced = local.updatedAt
            }
            if (snap?.metadata?.hasPendingWrites() != true) _status.value = SyncStatus.Synced
        }

        uploadJob = scope.launch {
            store.data.map { it.updatedAt }.distinctUntilChanged().debounce(1500).collect { at ->
                if (at > lastSynced) upload(doc, store.data.value)
            }
        }
    }

    private fun upload(doc: DocumentReference, data: AppData) {
        lastSynced = data.updatedAt
        _status.value = SyncStatus.Syncing
        doc.set(mapOf("json" to store.encode(data), "updatedAt" to data.updatedAt))
            .addOnSuccessListener { _status.value = SyncStatus.Synced }
            .addOnFailureListener {
                Log.w(TAG, "Yükleme hatası", it)
                _status.value = SyncStatus.Error
            }
    }

    companion object {
        private const val TAG = "QuelleSync"
    }
}
