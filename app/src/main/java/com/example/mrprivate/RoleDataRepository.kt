package com.example.mrprivate

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// ============================================================================
// 1) قائمة ذات "ملكية" للعناصر: كل مصدر (استعلام) يملك عناصره، فلا يحذف مصدر عناصر مصدر آخر
// ============================================================================
private class OwnedList<T>(
    private val list: SnapshotStateList<T>,
    private val idOf: (T) -> String
) {
    private val ownersById = HashMap<String, MutableSet<String>>()
    private val idsByOwner = HashMap<String, Set<String>>()

    /** يستبدل مجموعة عناصر المصدر بالكامل (يحذف ما اختفى، ويضيف/يحدّث الباقي) */
    fun replace(owner: String, items: List<T>) {
        val newIds = items.map(idOf).toHashSet()
        val old = idsByOwner[owner] ?: emptySet()
        for (id in old) {
            if (id in newIds) continue
            val set = ownersById[id]
            set?.remove(owner)
            if (set == null || set.isEmpty()) {
                ownersById.remove(id)
                list.removeAll { idOf(it) == id }
            }
        }
        applyUpserts(owner, items)
        idsByOwner[owner] = newIds
    }

    /** يضيف/يحدّث عناصر دون حذف أي شيء (للجلب المفرد get()) */
    fun upsert(owner: String, items: List<T>) {
        applyUpserts(owner, items)
        idsByOwner[owner] = (idsByOwner[owner] ?: emptySet()) + items.map(idOf)
    }

    fun clearOwner(owner: String) = replace(owner, emptyList())

    fun clearAll() {
        ownersById.clear()
        idsByOwner.clear()
        list.clear()
    }

    private fun applyUpserts(owner: String, items: List<T>) {
        // خريطة فهرس للبحث O(1) بدل find داخل حلقة
        val index = HashMap<String, Int>(list.size * 2)
        list.forEachIndexed { i, item -> index[idOf(item)] = i }
        for (item in items) {
            val id = idOf(item)
            ownersById.getOrPut(id) { HashSet() }.add(owner)
            val at = index[id]
            if (at == null) {
                list.add(item)
                index[id] = list.size - 1
            } else if (list[at] != item) {
                list[at] = item
            }
        }
    }
}

// ============================================================================
// 2) مساعدات القراءة
// ============================================================================
/** نفس منطق القراءة القديم (يدعم الحقول البديلة approved / premium) */
fun parseTeacherSnapshot(child: DataSnapshot): TeacherProfile? {
    val approvedFlag = child.child("isApproved").getValue(Boolean::class.java)
        ?: child.child("approved").getValue(Boolean::class.java) ?: false
    val premiumFlag = child.child("isPremium").getValue(Boolean::class.java)
        ?: child.child("premium").getValue(Boolean::class.java) ?: false
    return child.getValue(TeacherProfile::class.java)?.let {
        // المعرّف = مفتاح العقدة = uid الخاص بحساب Firebase Auth
        it.copy(id = child.key ?: it.id, isApproved = approvedFlag || it.isApproved, isPremium = premiumFlag || it.isPremium)
    }
}

fun parseStudentSnapshot(child: DataSnapshot): StudentProfile? =
    child.getValue(StudentProfile::class.java)?.let { it.copy(id = child.key ?: it.id) }

fun parseAdminSnapshot(child: DataSnapshot): AdminAccount? =
    child.getValue(AdminAccount::class.java)?.let { it.copy(id = child.key ?: it.id) }

/** يحوّل أي Query إلى Flow، ويزيل المستمع تلقائياً عند الإلغاء (awaitClose) فلا يحدث تسريب */
private fun Query.snapshotFlow(): Flow<DataSnapshot> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot) }
        override fun onCancelled(error: DatabaseError) { close(error.toException()) }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

// ============================================================================
// 3) مستودع البيانات حسب الدور (Role-Based Queries)
// ============================================================================
/**
 * المعلم : مواعيده (teacherId) + كويزاته (teacherId) + الطلاب المسجلون في مواعيده فقط.
 * ولي الأمر: أبناؤه + مواعيد أبنائه (عبر فهرس student_slots) + معلمو تلك المواعيد.
 * الطالب : بياناته + مواعيده (عبر student_slots) + معلموه.
 * الدليل : المعلمون المعتمدون (isApproved == true) بصفحات.
 * الأدمن : تحميل كامل (مؤقتاً) — يُنقل لاحقاً إلى صفحات.
 */
class RoleDataRepository(
    private val state: MrPrivateState,
    private val db: FirebaseDatabase
) {
    companion object {
        const val DIRECTORY_PAGE_SIZE = 100
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = HashMap<String, Job>()
    private var sessionToken = 0

    private val adminsOwned = OwnedList(state.admins) { it.id }
    private val teachersOwned = OwnedList(state.teachers) { it.id }
    private val studentsOwned = OwnedList(state.students) { it.id }
    private val schedulesOwned = OwnedList(state.schedules) { it.id }
    private val quizzesOwned = OwnedList(state.quizzes) { it.id }

    private val fetchedStudents = HashSet<String>()
    private val fetchedTeachers = HashSet<String>()
    private val indexedPairs = HashSet<String>()

    // حالة نطاق الطالب / ولي الأمر
    private var currentParent: StudentProfile? = null
    private var watchedIndexStudents: Set<String> = emptySet()
    private val indexSlotIds = HashMap<String, Set<String>>()
    private val pendingIndexInitial = HashSet<String>()
    private val activeSlotIds = HashSet<String>()
    private val pendingSlotInitial = HashSet<String>()
    private val slotTeacher = HashMap<String, String>()

    // حالة دليل المعلمين
    private var directoryLastKey: String? = null
    private var loadingMoreDirectory = false
    private var browsingTeacherId: String? = null

    // ---------------------------------------------------------------- أدوات داخلية
    private fun watch(
        key: String,
        query: Query,
        onError: () -> Unit = {},
        onSnapshot: (DataSnapshot) -> Unit
    ) {
        jobs.remove(key)?.cancel()
        jobs[key] = scope.launch {
            query.snapshotFlow()
                .catch { onError() }
                .collect { onSnapshot(it) }
        }
    }

    private fun stopKey(key: String) { jobs.remove(key)?.cancel() }

    private fun markLoaded() {
        state.studentsLoaded = true
        state.schedulesLoaded = true
    }

    // ---------------------------------------------------------------- مراقبات عامة (طوال عمر التطبيق)
    fun startGlobalWatchers() {
        watch("g:conn", db.getReference(".info/connected")) {
            state.isDbConnected = it.getValue(Boolean::class.java) == true
        }
    }

    // ---------------------------------------------------------------- إدارة الجلسة
    fun startSession(kind: String, userId: String) {
        stopSession()
        state.studentsLoaded = false
        state.schedulesLoaded = false
        // إعدادات الترم (تحتاج مستخدماً مسجلاً)
        watch("s:terms", db.getReference("settings/terms")) { state.applyTermsSnapshot(it) }
        when (kind) {
            "teacher" -> startTeacher(userId)
            "admin" -> startAdmin()
            else -> startFamilyMember(userId)
        }
    }

    fun stopSession() {
        sessionToken++
        jobs.keys.filter { it.startsWith("s:") }.forEach { stopKey(it) }
        fetchedStudents.clear()
        fetchedTeachers.clear()
        indexedPairs.clear()
        currentParent = null
        watchedIndexStudents = emptySet()
        indexSlotIds.clear()
        pendingIndexInitial.clear()
        activeSlotIds.clear()
        pendingSlotInitial.clear()
        slotTeacher.clear()
        directoryLastKey = null
        loadingMoreDirectory = false
        browsingTeacherId = null
        adminsOwned.clearAll()
        teachersOwned.clearAll()
        studentsOwned.clearAll()
        schedulesOwned.clearAll()
        quizzesOwned.clearAll()
        state.hasMoreTeachers = false
    }

    /** يضع ملف المستخدم الذي تحقق منه تسجيل الدخول فوراً في القائمة (قبل وصول أول لقطة من الاستماع) */
    fun preloadAdmin(admin: AdminAccount) = adminsOwned.upsert("self", listOf(admin))
    fun preloadTeacher(teacher: TeacherProfile) = teachersOwned.upsert("self", listOf(teacher))
    fun preloadStudent(student: StudentProfile) = studentsOwned.upsert("self", listOf(student))

    // ---------------------------------------------------------------- المعلم
    private fun startTeacher(teacherId: String) {
        watch("s:t:self", db.getReference("teachers/$teacherId"), onError = { markLoaded() }) { snap ->
            parseTeacherSnapshot(snap)?.let { teachersOwned.replace("self", listOf(it)) }
        }
        watch(
            "s:t:slots",
            db.getReference("schedules").orderByChild("teacherId").equalTo(teacherId),
            onError = { markLoaded() }
        ) { snap ->
            val slots = snap.children.mapNotNull { it.getValue(ScheduleSlot::class.java) }
            schedulesOwned.replace("slots", slots)
            state.schedulesLoaded = true
            slots.forEach { indexSlot(it) } // تعبئة فهرس student_slots للبيانات القديمة (مرة واحدة لكل زوج)
            val studentIds = slots.flatMap { s -> s.enrollments.map { it.studentId } }.toSet()
            fetchStudentsOnce(studentIds) { state.studentsLoaded = true }
        }
        watch("s:t:quizzes", db.getReference("quizzes").orderByChild("teacherId").equalTo(teacherId)) { snap ->
            quizzesOwned.replace("quizzes", snap.children.mapNotNull { it.getValue(QuizExam::class.java) })
            state.purgeExpiredPapers()
        }
    }

    // ---------------------------------------------------------------- الطالب / ولي الأمر
    private fun startFamilyMember(userId: String) {
        watch("s:u:self", db.getReference("students/$userId"), onError = { markLoaded() }) { snap ->
            val me = parseStudentSnapshot(snap)
            if (me == null) {
                markLoaded()
                return@watch
            }
            studentsOwned.replace("self", listOf(me))
            if (me.isParent) startParentScope(me) else startStudentScope(me)
        }
        startDirectory()
    }

    private fun startStudentScope(me: StudentProfile) {
        if (me.parentId.isNotBlank()) fetchStudentsOnce(setOf(me.parentId))
        watchSlotIndex(setOf(me.id))
        state.studentsLoaded = true
    }

    private fun isChildOf(parent: StudentProfile, s: StudentProfile): Boolean =
        s.parentId == parent.id ||
            parent.registeredStudentIds.contains(s.id) ||
            (parent.linkedStudentUsername.isNotBlank() && s.username.equals(parent.linkedStudentUsername.trim(), true))

    private fun startParentScope(me: StudentProfile) {
        currentParent = me
        if (!jobs.containsKey("s:p:kids")) {
            watch("s:p:kids", db.getReference("students").orderByChild("parentId").equalTo(me.id), onError = { markLoaded() }) { snap ->
                studentsOwned.replace("kids", snap.children.mapNotNull { parseStudentSnapshot(it) })
                onChildrenChanged()
            }
        }
        fetchStudentsOnce(me.registeredStudentIds.toSet()) { onChildrenChanged() }
        state.studentsLoaded = true
    }

    private fun onChildrenChanged() {
        val parent = currentParent ?: return
        val childIds = state.students.filter { it.id != parent.id && isChildOf(parent, it) }.map { it.id }.toSet()
        watchSlotIndex(childIds)
    }

    /** يراقب فهرس student_slots/{studentId} ثم المواعيد المرتبطة به فقط (بدل تحميل كل المواعيد) */
    private fun watchSlotIndex(studentIds: Set<String>) {
        (watchedIndexStudents - studentIds).forEach { sid ->
            stopKey("s:idx:$sid")
            indexSlotIds.remove(sid)
        }
        val added = studentIds - watchedIndexStudents
        watchedIndexStudents = studentIds
        if (studentIds.isEmpty()) {
            reconcileSlotWatchers()
            return
        }
        added.forEach { sid ->
            pendingIndexInitial.add(sid)
            watch(
                "s:idx:$sid",
                db.getReference("student_slots/$sid"),
                onError = {
                    pendingIndexInitial.remove(sid)
                    maybeSchedulesLoaded()
                }
            ) { snap ->
                indexSlotIds[sid] = snap.children.mapNotNull { it.key }.toSet()
                pendingIndexInitial.remove(sid)
                reconcileSlotWatchers()
            }
        }
        reconcileSlotWatchers()
    }

    private fun reconcileSlotWatchers() {
        val wanted = indexSlotIds.values.flatten().toSet()
        (activeSlotIds - wanted).forEach { id ->
            stopKey("s:slot:$id")
            schedulesOwned.clearOwner("slot:$id")
            slotTeacher.remove(id)
            activeSlotIds.remove(id)
        }
        (wanted - activeSlotIds).forEach { id ->
            activeSlotIds.add(id)
            pendingSlotInitial.add(id)
            watch(
                "s:slot:$id",
                db.getReference("schedules/$id"),
                onError = {
                    pendingSlotInitial.remove(id)
                    maybeSchedulesLoaded()
                }
            ) { snap ->
                val slot = snap.getValue(ScheduleSlot::class.java)
                // نعرض الموعد فقط إن كان أحد المراقَبين مسجلاً فيه فعلاً (فهرس قديم/ملغي لا يظهر)
                val visible = slot != null && slot.enrollments.any { it.studentId in watchedIndexStudents }
                if (visible && slot != null) {
                    schedulesOwned.replace("slot:$id", listOf(slot))
                    slotTeacher[id] = slot.teacherId
                } else {
                    schedulesOwned.clearOwner("slot:$id")
                    slotTeacher.remove(id)
                }
                pendingSlotInitial.remove(id)
                maybeSchedulesLoaded()
                onSlotsChanged()
            }
        }
        maybeSchedulesLoaded()
        onSlotsChanged()
    }

    private fun maybeSchedulesLoaded() {
        if (pendingIndexInitial.isEmpty() && pendingSlotInitial.isEmpty()) state.schedulesLoaded = true
    }

    /** معلمو المواعيد المحجوزة: نجلب ملفاتهم (مرة واحدة) ونراقب كويزاتهم فقط */
    private fun onSlotsChanged() {
        val teacherIds = slotTeacher.values.filter { it.isNotBlank() }.toSet()
        fetchTeachersOnce(teacherIds)
        teacherIds.forEach { tid ->
            val key = "s:quiz:$tid"
            if (!jobs.containsKey(key)) {
                watch(key, db.getReference("quizzes").orderByChild("teacherId").equalTo(tid)) { snap ->
                    quizzesOwned.replace("quiz:$tid", snap.children.mapNotNull { it.getValue(QuizExam::class.java) })
                }
            }
        }
    }

    // ---------------------------------------------------------------- دليل المعلمين (صفحات)
    fun startDirectory() {
        watch(
            "s:dir",
            db.getReference("teachers").orderByChild("isApproved").equalTo(true).limitToFirst(DIRECTORY_PAGE_SIZE)
        ) { snap ->
            val list = snap.children.mapNotNull { parseTeacherSnapshot(it) }
            teachersOwned.replace("dir", list)
            directoryLastKey = snap.children.lastOrNull()?.key
            state.hasMoreTeachers = list.size >= DIRECTORY_PAGE_SIZE
        }
    }

    /** الصفحة التالية من الدليل (Cursor-based): تُستدعى من الواجهة عند الوصول لنهاية القائمة */
    fun loadMoreDirectory() {
        val last = directoryLastKey ?: return
        if (!state.hasMoreTeachers || loadingMoreDirectory) return
        loadingMoreDirectory = true
        val token = sessionToken
        db.getReference("teachers")
            .orderByChild("isApproved").startAt(true, last).endAt(true)
            .limitToFirst(DIRECTORY_PAGE_SIZE + 1)
            .get()
            .addOnCompleteListener { task ->
                loadingMoreDirectory = false
                if (token != sessionToken || !task.isSuccessful) return@addOnCompleteListener
                val children = task.result.children.toList()
                val items = children.filter { it.key != last }.mapNotNull { parseTeacherSnapshot(it) }
                teachersOwned.upsert("dirMore", items)
                directoryLastKey = children.lastOrNull()?.key ?: last
                state.hasMoreTeachers = items.size >= DIRECTORY_PAGE_SIZE
            }
    }

    /** عند فتح الطالب لمعلم معيّن: نجلب مواعيده هو فقط (بدل كل المواعيد) */
    fun listenTeacherSlots(teacherId: String?) {
        browsingTeacherId?.let {
            stopKey("s:browse:$it")
            schedulesOwned.clearOwner("browse:$it")
        }
        browsingTeacherId = teacherId
        if (teacherId.isNullOrBlank()) return
        watch("s:browse:$teacherId", db.getReference("schedules").orderByChild("teacherId").equalTo(teacherId)) { snap ->
            schedulesOwned.replace("browse:$teacherId", snap.children.mapNotNull { it.getValue(ScheduleSlot::class.java) })
        }
    }

    // ---------------------------------------------------------------- الأدمن (تحميل كامل مؤقتاً)
    private fun startAdmin() {
        watch("s:a:admins", db.getReference("admins")) { snap ->
            adminsOwned.replace("admin", snap.children.mapNotNull { parseAdminSnapshot(it) })
        }
        watch("s:a:teachers", db.getReference("teachers"), onError = { markLoaded() }) { snap ->
            teachersOwned.replace("admin", snap.children.mapNotNull { parseTeacherSnapshot(it) })
        }
        watch("s:a:students", db.getReference("students"), onError = { markLoaded() }) { snap ->
            studentsOwned.replace("admin", snap.children.mapNotNull { parseStudentSnapshot(it) })
            state.studentsLoaded = true
        }
        watch("s:a:schedules", db.getReference("schedules"), onError = { markLoaded() }) { snap ->
            val slots = snap.children.mapNotNull { it.getValue(ScheduleSlot::class.java) }
            schedulesOwned.replace("admin", slots)
            state.schedulesLoaded = true
            slots.forEach { indexSlot(it) }
        }
        watch("s:a:quizzes", db.getReference("quizzes")) { snap ->
            quizzesOwned.replace("admin", snap.children.mapNotNull { it.getValue(QuizExam::class.java) })
            state.purgeExpiredPapers()
        }
    }

    // ---------------------------------------------------------------- جلب مفرد (get) للبيانات غير المتغيرة كثيراً
    private fun fetchStudentsOnce(ids: Set<String>, onDone: () -> Unit = {}) {
        val toFetch = ids.filter { it.isNotBlank() && fetchedStudents.add(it) }
        if (toFetch.isEmpty()) { onDone(); return }
        val token = sessionToken
        var remaining = toFetch.size
        toFetch.forEach { id ->
            db.getReference("students/$id").get().addOnCompleteListener { task ->
                if (token != sessionToken) return@addOnCompleteListener
                if (task.isSuccessful) {
                    parseStudentSnapshot(task.result)?.let { studentsOwned.upsert("fetched", listOf(it)) }
                } else {
                    fetchedStudents.remove(id) // للسماح بإعادة المحاولة لاحقاً
                }
                remaining--
                if (remaining == 0) onDone()
            }
        }
    }

    private fun fetchTeachersOnce(ids: Set<String>) {
        val token = sessionToken
        ids.filter { id -> id.isNotBlank() && state.teachers.none { it.id == id } && fetchedTeachers.add(id) }.forEach { id ->
            db.getReference("teachers/$id").get().addOnCompleteListener { task ->
                if (token != sessionToken) return@addOnCompleteListener
                if (task.isSuccessful) {
                    parseTeacherSnapshot(task.result)?.let { teachersOwned.upsert("fetched", listOf(it)) }
                } else {
                    fetchedTeachers.remove(id)
                }
            }
        }
    }

    // ---------------------------------------------------------------- فهرس student_slots (كتابة)
    /** يكتب student_slots/{studentId}/{slotId} لكل طالب مسجل (مرة واحدة لكل زوج في الجلسة) */
    fun indexSlot(slot: ScheduleSlot) {
        val updates = HashMap<String, Any?>()
        slot.enrollments.map { it.studentId }.filter { it.isNotBlank() }.distinct().forEach { sid ->
            if (indexedPairs.add("$sid|${slot.id}")) {
                updates["student_slots/$sid/${slot.id}"] = true
                // فهرس يسمح للمعلم بقراءة ملفات طلابه المسجلين عنده فقط
                if (slot.teacherId.isNotBlank()) updates["teacher_students/${slot.teacherId}/$sid"] = true
            }
        }
        if (updates.isNotEmpty()) db.reference.updateChildren(updates)
    }

    fun unindexSlot(slot: ScheduleSlot) {
        val updates = HashMap<String, Any?>()
        slot.enrollments.map { it.studentId }.filter { it.isNotBlank() }.distinct().forEach { sid ->
            indexedPairs.remove("$sid|${slot.id}")
            updates["student_slots/$sid/${slot.id}"] = null
        }
        if (updates.isNotEmpty()) db.reference.updateChildren(updates)
    }

    fun unindexStudent(slotId: String, studentId: String) {
        if (slotId.isBlank() || studentId.isBlank()) return
        indexedPairs.remove("$studentId|$slotId")
        db.getReference("student_slots/$studentId/$slotId").removeValue()
    }
}

// ============================================================================
// 4) نتيجة تسجيل الدخول
// ============================================================================
enum class LoginResult { SUCCESS, WRONG_CREDENTIALS, NETWORK_ERROR }
