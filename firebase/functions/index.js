// اختياري ولكنه الضمان الوحيد لحذف الصور حتى لو لم يفتح أحد التطبيق (يتطلب خطة Blaze)
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
admin.initializeApp();

exports.purgeExpiredQuizPapers = onSchedule("every 10 minutes", async () => {
  const ref = admin.database().ref("quiz_papers");
  const snap = await ref.orderByChild("expiresAt").endAt(Date.now()).get();
  const updates = {};
  snap.forEach((child) => { updates[child.key] = null; });
  if (Object.keys(updates).length) await ref.update(updates);
});
