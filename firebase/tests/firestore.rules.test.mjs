// Checks that firestore.rules allow the real app flows and block everything else.
import { test, before, after, beforeEach } from "node:test";
import { readFileSync } from "node:fs";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import {
  doc, getDoc, setDoc, updateDoc, deleteDoc, collection, getDocs,
  writeBatch, runTransaction, serverTimestamp, Timestamp, deleteField,
} from "firebase/firestore";

let env;
const CODE = "ABC234";

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-lovelyy",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});
after(async () => env.cleanup());
beforeEach(async () => env.clearFirestore());

const db = (uid) => env.authenticatedContext(uid).firestore();

async function signUp(uid, name) {
  await assertSucceeds(setDoc(doc(db(uid), "users", uid), {
    displayName: name, photoUrl: null, coupleId: null, pairCode: null, createdAt: serverTimestamp(),
  }));
}

async function createCode(uid, code = CODE) {
  const d = db(uid);
  const b = writeBatch(d);
  b.set(doc(d, "pairCodes", code), { ownerUid: uid, createdAt: serverTimestamp() });
  b.update(doc(d, "users", uid), { pairCode: code });
  return b.commit();
}

// Same transaction the Android app runs (PairingRepository.joinWithCode).
function join(uid, code = CODE) {
  const d = db(uid);
  return runTransaction(d, async (tx) => {
    const snap = await tx.get(doc(d, "pairCodes", code));
    if (!snap.exists()) throw new Error("no code");
    const owner = snap.data().ownerUid;
    tx.set(doc(d, "couples", code), { members: [owner, uid], createdAt: serverTimestamp(), startDate: null });
    tx.update(doc(d, "users", owner), { coupleId: code, pairCode: null });
    tx.update(doc(d, "users", uid), { coupleId: code, pairCode: null });
    tx.delete(doc(d, "pairCodes", code));
  });
}

// ---------- shared feature writes, shaped exactly like the Android repositories ----------
const sub = (uid, name, ...path) => doc(db(uid), "couples", CODE, name, ...path);
const note = (by, byName, extra = {}) => ({ text: "hi", color: 2, pinned: false, by, byName, at: Date.now(), ...extra });

// SignalLimits.next: the next limits/{uid} doc, from the current one and the phone's clock.
const HOUR = 3600_000, DAY = 24 * HOUR;
function nextLimits(cur, now = Date.now(), flip = false) {
  const win = (startKey, countKey, len) => {
    const start = cur?.[startKey]?.toMillis?.();
    let reset = start == null || now >= start + len;
    if (flip && start != null) reset = !reset;
    return reset ? { [startKey]: serverTimestamp(), [countKey]: 1 } : { [startKey]: cur[startKey], [countKey]: (cur[countKey] ?? 0) + 1 };
  };
  return { signalAt: serverTimestamp(), ...win("hourStart", "hourCount", HOUR), ...win("dayStart", "dayCount", DAY) };
}

// SignalRepository.send: the signal plus my rate-limit doc, in one batch.
async function sendSignal(from, to, extra = {}, { flip = false, limits } = {}) {
  const d = db(from);
  const limRef = doc(d, "couples", CODE, "limits", from);
  const cur = (await getDoc(limRef)).data();
  const b = writeBatch(d);
  b.set(limRef, limits ?? nextLimits(cur, Date.now(), flip));
  b.set(doc(collection(d, "couples", CODE, "signals")), {
    kind: "heartbeat", emotion: "sevgi", mood: null, from, fromName: "Alice", to, text: null,
    at: Date.now(), createdAt: serverTimestamp(), ...extra,
  });
  return b.commit();
}

// Sets my limits doc directly (as if earlier signals happened), bypassing the rules.
async function setLimits(uid, data) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), "couples", CODE, "limits", uid), data);
  });
}

// QuestionRepository.answer (merge write).
function answer(uid, day = "2026-11-26", text = "Seni") {
  return setDoc(sub(uid, "questions", day), {
    question: "Bugün beni ne zaman düşündün?",
    answers: { [uid]: { name: uid, text, at: Date.now() } },
    updatedAt: serverTimestamp(),
  }, { merge: true });
}

async function pairAB() {
  await signUp("alice", "Alice");
  await signUp("bob", "Bob");
  await assertSucceeds(createCode("alice"));
  await assertSucceeds(join("bob"));
}

test("happy path: share code, partner joins, both linked", async () => {
  await pairAB();
  const a = (await getDoc(doc(db("alice"), "users", "alice"))).data();
  const b = (await getDoc(doc(db("bob"), "users", "bob"))).data();
  if (a.coupleId !== CODE || b.coupleId !== CODE) throw new Error("not linked");
  if (a.pairCode !== null) throw new Error("code not cleared");
  await assertSucceeds(getDoc(doc(db("alice"), "couples", CODE)));
  await assertSucceeds(getDoc(doc(db("alice"), "users", "bob")));   // partner profile
  await assertSucceeds(getDoc(doc(db("bob"), "users", "alice")));
  const code = await getDoc(doc(db("bob"), "pairCodes", CODE));
  if (code.exists()) throw new Error("code should be consumed");
});

test("strangers cannot read users, couples or list codes", async () => {
  await pairAB();
  await signUp("eve", "Eve");
  await assertFails(getDoc(doc(db("eve"), "users", "alice")));
  await assertFails(getDoc(doc(db("eve"), "couples", CODE)));
  await assertFails(getDocs(collection(db("eve"), "pairCodes")));
  await assertFails(getDocs(collection(db("eve"), "users")));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), "users", "alice")));
});

test("couple subcollections are members-only", async () => {
  await pairAB();
  await signUp("eve", "Eve");
  await assertSucceeds(setDoc(doc(db("alice"), "couples", CODE, "notes", "n1"), note("alice", "Alice")));
  await assertSucceeds(getDoc(doc(db("bob"), "couples", CODE, "notes", "n1")));
  await assertFails(getDoc(doc(db("eve"), "couples", CODE, "notes", "n1")));
  await assertFails(getDocs(collection(db("eve"), "couples", CODE, "notes")));
  await assertFails(setDoc(doc(db("eve"), "couples", CODE, "notes", "n2"), note("eve", "Eve")));
  await assertFails(deleteDoc(doc(db("eve"), "couples", CODE, "notes", "n1")));
});

test("cannot join without a valid code or twice", async () => {
  await pairAB();
  await signUp("eve", "Eve");
  // Code was consumed.
  await join("eve").then(() => { throw new Error("should fail"); }, () => {});
  // Forging a couple directly with Alice.
  const d = db("eve");
  const b = writeBatch(d);
  b.set(doc(d, "couples", "ZZZ999"), { members: ["alice", "eve"], createdAt: serverTimestamp(), startDate: null });
  b.update(doc(d, "users", "alice"), { coupleId: "ZZZ999" });
  b.update(doc(d, "users", "eve"), { coupleId: "ZZZ999" });
  await assertFails(b.commit());
  // Paired users cannot make new codes.
  await assertFails(createCode("alice", "QQQ222"));
});

test("paired user cannot be stolen with a fresh code", async () => {
  await pairAB();
  await signUp("eve", "Eve");
  await assertSucceeds(createCode("eve", "EEE333"));
  await assertFails(join("bob", "EEE333"));
});

test("own code and expired code are rejected", async () => {
  await signUp("alice", "Alice");
  await signUp("bob", "Bob");
  await assertSucceeds(createCode("alice"));
  await assertFails(join("alice"));
  await env.withSecurityRulesDisabled(async (ctx) => {
    await updateDoc(doc(ctx.firestore(), "pairCodes", CODE), {
      createdAt: Timestamp.fromMillis(Date.now() - 25 * 3600 * 1000),
    });
  });
  await assertFails(join("bob"));
});

test("members cannot change membership; profile rules hold", async () => {
  await pairAB();
  await assertFails(updateDoc(doc(db("alice"), "couples", CODE), { members: ["alice", "eve"] }));
  await assertSucceeds(updateDoc(doc(db("alice"), "couples", CODE), { startDate: Timestamp.now() }));
  await assertFails(updateDoc(doc(db("alice"), "users", "bob"), { displayName: "Hacked" }));
  await assertFails(updateDoc(doc(db("alice"), "users", "alice"), { coupleId: null }));
  await assertSucceeds(updateDoc(doc(db("alice"), "users", "alice"), { displayName: "Ali" }));
  await assertFails(updateDoc(doc(db("alice"), "users", "alice"), { displayName: "x".repeat(41) }));
  await assertFails(deleteDoc(doc(db("alice"), "couples", CODE)));
});

test("creating a new code retires the old one", async () => {
  await signUp("alice", "Alice");
  await assertSucceeds(createCode("alice", "AAA222"));
  const d = db("alice");
  const b = writeBatch(d);
  b.delete(doc(d, "pairCodes", "AAA222"));
  b.set(doc(d, "pairCodes", "BBB333"), { ownerUid: "alice", createdAt: serverTimestamp() });
  b.update(doc(d, "users", "alice"), { pairCode: "BBB333" });
  await assertSucceeds(b.commit());
  // Cannot claim someone else's code in your profile, or use lowercase/ambiguous codes.
  await assertFails(createCode("alice", "abc234"));
  await assertFails(createCode("alice", "OOO111"));
});

test("joiner who was also sharing a code can still pair", async () => {
  await signUp("alice", "Alice");
  await signUp("bob", "Bob");
  await assertSucceeds(createCode("alice"));
  await assertSucceeds(createCode("bob", "BBB234"));
  const d = db("bob");
  await assertSucceeds(runTransaction(d, async (tx) => {
    await tx.get(doc(d, "pairCodes", CODE));
    tx.set(doc(d, "couples", CODE), { members: ["alice", "bob"], createdAt: serverTimestamp(), startDate: null });
    tx.update(doc(d, "users", "alice"), { coupleId: CODE, pairCode: null });
    tx.update(doc(d, "users", "bob"), { coupleId: CODE, pairCode: null });
    tx.delete(doc(d, "pairCodes", CODE));
    tx.delete(doc(d, "pairCodes", "BBB234"));
  }));
});

// ---------- unpairing (LeaveRepository) ----------

// What the leaving partner's app does: delete shared docs, then end the couple and leave it in one batch.
async function endAsAlice() {
  const d = db("alice");
  const b = writeBatch(d);
  b.update(doc(d, "couples", CODE), { endedAt: serverTimestamp(), endedBy: "alice", endedByName: "Alice" });
  b.update(doc(d, "users", "alice"), { coupleId: null });
  return b.commit();
}

// What the partner who stayed does when they see the ended couple.
function leaveAsBob() {
  const d = db("bob");
  const b = writeBatch(d);
  b.update(doc(d, "users", "bob"), { coupleId: null });
  b.delete(doc(d, "couples", CODE));
  return b.commit();
}

test("unpair: delete shared data, end, partner leaves, both can pair again", async () => {
  await pairAB();
  await assertSucceeds(setDoc(doc(db("alice"), "couples", CODE, "notes", "n1"), note("alice", "Alice")));
  await assertSucceeds(deleteDoc(doc(db("alice"), "couples", CODE, "notes", "n1")));
  await assertSucceeds(endAsAlice());
  // Ended: nothing new can be written, deletes still work, reads still work for members.
  await assertFails(setDoc(doc(db("bob"), "couples", CODE, "notes", "n2"), note("bob", "Bob")));
  await assertFails(sendSignal("bob", "alice"));
  await assertSucceeds(getDoc(doc(db("bob"), "couples", CODE)));
  await assertSucceeds(leaveAsBob());
  const a = await getDoc(doc(db("alice"), "users", "alice"));
  const b = await getDoc(doc(db("bob"), "users", "bob"));
  if (a.data().coupleId !== null || b.data().coupleId !== null) throw new Error("still paired");
  await assertSucceeds(createCode("alice", "DEF345"));
});

test("unpair: cannot leave a couple that is not ended, or end it for someone else", async () => {
  await pairAB();
  await assertFails(updateDoc(doc(db("alice"), "users", "alice"), { coupleId: null }));
  await assertFails(deleteDoc(doc(db("alice"), "couples", CODE)));
  await assertFails(updateDoc(doc(db("alice"), "couples", CODE), { endedAt: serverTimestamp(), endedBy: "bob", endedByName: "Bob" }));
  await assertFails(updateDoc(doc(db("alice"), "couples", CODE), { endedAt: Timestamp.fromMillis(1), endedBy: "alice", endedByName: "Alice" }));
  await signUp("eve", "Eve");
  await assertFails(updateDoc(doc(db("eve"), "couples", CODE), { endedAt: serverTimestamp(), endedBy: "eve", endedByName: "Eve" }));
  await assertSucceeds(endAsAlice());
  // Once ended it can't be ended again or have its start date changed.
  await assertFails(updateDoc(doc(db("bob"), "couples", CODE), { endedAt: serverTimestamp(), endedBy: "bob", endedByName: "Bob" }));
  await assertFails(updateDoc(doc(db("bob"), "couples", CODE), { startDate: Timestamp.now() }));
  await assertFails(deleteDoc(doc(db("eve"), "couples", CODE)));
});

test("delete account: only my own doc, only when not paired", async () => {
  await pairAB();
  await assertFails(deleteDoc(doc(db("alice"), "users", "alice")));
  await assertSucceeds(endAsAlice());
  await assertFails(deleteDoc(doc(db("alice"), "users", "bob")));
  await assertSucceeds(deleteDoc(doc(db("alice"), "users", "alice")));
  // The partner who stayed can still leave after the other account is gone.
  await assertSucceeds(leaveAsBob());
});

// ---------- shared features: field validation, authorship, rate limit ----------

test("signals: valid send works, forged sender / recipient / kind are rejected", async () => {
  await pairAB();
  await assertSucceeds(sendSignal("alice", "bob"));
  await assertFails(sendSignal("alice", "alice"));                       // to myself
  await assertFails(sendSignal("alice", "eve"));                         // not my partner
  await assertFails(sendSignal("alice", "bob", { from: "bob" }));        // pretending to be Bob
  await assertFails(sendSignal("alice", "bob", { kind: "spam" }));
  await assertFails(sendSignal("alice", "bob", { text: "x".repeat(121) }));
  await assertFails(sendSignal("alice", "bob", { extra: true }));
  await assertFails(sendSignal("alice", "bob", { createdAt: Timestamp.fromMillis(1) }));
  // A signal without the rate-limit stamp in the same batch is rejected.
  await assertFails(setDoc(doc(collection(db("alice"), "couples", CODE, "signals")), {
    kind: "nudge", emotion: null, mood: null, from: "alice", fromName: "Alice", to: "bob", text: null,
    at: Date.now(), createdAt: serverTimestamp(),
  }));
  // Signals are never edited.
  const d = db("alice");
  const ref = doc(collection(d, "couples", CODE, "signals"));
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), ref.path), { kind: "nudge", from: "alice", to: "bob" });
  });
  await assertFails(updateDoc(ref, { kind: "heartbeat" }));
});

test("signals: rate limit is one per 2 seconds per person", async () => {
  await pairAB();
  await assertSucceeds(sendSignal("alice", "bob"));
  await assertFails(sendSignal("alice", "bob"));          // right away again
  await assertSucceeds(sendSignal("bob", "alice", { fromName: "Bob" })); // the partner has their own limit
  // Two seconds later it works again (simulated by moving the stamps back).
  const ago = (ms) => Timestamp.fromMillis(Date.now() - ms);
  await setLimits("alice", { signalAt: ago(3000), hourStart: ago(3000), hourCount: 1, dayStart: ago(3000), dayCount: 1 });
  await assertSucceeds(sendSignal("alice", "bob"));
  // Nobody can reset someone else's stamp or write an arbitrary one.
  await assertFails(setDoc(sub("bob", "limits", "alice"), nextLimits(null)));
  await assertFails(setDoc(sub("alice", "limits", "alice"), { ...nextLimits(null), signalAt: Timestamp.fromMillis(1) }));
});

test("signals: at most 15 per hour and 60 per day by default, windows restart in server time", async () => {
  await pairAB();
  const ago = (ms) => Timestamp.fromMillis(Date.now() - ms);
  // 15 already sent this hour: the 16th is refused, also when trying to start a new window early.
  await setLimits("alice", { signalAt: ago(5000), hourStart: ago(10 * 60_000), hourCount: 15, dayStart: ago(HOUR), dayCount: 20 });
  await assertFails(sendSignal("alice", "bob"));
  await assertFails(sendSignal("alice", "bob", {}, { flip: true }));
  // An hour later a new hourly window starts; the day keeps counting.
  await setLimits("alice", { signalAt: ago(5000), hourStart: ago(HOUR + 1000), hourCount: 15, dayStart: ago(2 * HOUR), dayCount: 20 });
  await assertSucceeds(sendSignal("alice", "bob"));
  const lim = (await getDoc(sub("alice", "limits", "alice"))).data();
  if (lim.hourCount !== 1 || lim.dayCount !== 21) throw new Error(`unexpected counts ${lim.hourCount}/${lim.dayCount}`);
  // 60 today: refused until the day window is over.
  await setLimits("alice", { signalAt: ago(5000), hourStart: ago(HOUR + 1000), hourCount: 3, dayStart: ago(20 * HOUR), dayCount: 60 });
  await assertFails(sendSignal("alice", "bob"));
  await setLimits("alice", { signalAt: ago(5000), hourStart: ago(HOUR + 1000), hourCount: 3, dayStart: ago(DAY + 1000), dayCount: 60 });
  await assertSucceeds(sendSignal("alice", "bob"));
  // A phone with a wrong clock that counts up in an expired window is still accepted (only stricter).
  await setLimits("alice", { signalAt: ago(5000), hourStart: ago(2 * HOUR), hourCount: 5, dayStart: ago(2 * DAY), dayCount: 5 });
  await assertSucceeds(sendSignal("alice", "bob", {}, { flip: true }));
  // Counts can't be faked: skipping numbers or resetting a running window is refused.
  await setLimits("alice", { signalAt: ago(5000), hourStart: ago(60_000), hourCount: 9, dayStart: ago(60_000), dayCount: 9 });
  const fake = { signalAt: serverTimestamp(), hourStart: ago(60_000), hourCount: 2, dayStart: ago(60_000), dayCount: 10 };
  await assertFails(sendSignal("alice", "bob", {}, { limits: fake }));
  await assertFails(sendSignal("alice", "bob", {}, { limits: nextLimits(null) }));
});

test("signal limits come from config/limits, which the app can read but never write", async () => {
  await pairAB();
  const ago = (ms) => Timestamp.fromMillis(Date.now() - ms);
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), "config", "limits"), { signalsPerHour: 3, signalsPerDay: 5, minGapSeconds: 2 });
  });
  await setLimits("alice", { signalAt: ago(5000), hourStart: ago(60_000), hourCount: 3, dayStart: ago(60_000), dayCount: 3 });
  await assertFails(sendSignal("alice", "bob"));                       // 3 per hour from config
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), "config", "limits"), { signalsPerHour: 100, signalsPerDay: 500 });
  });
  await assertSucceeds(sendSignal("alice", "bob"));                    // raised in the console
  await assertSucceeds(getDoc(doc(db("bob"), "config", "limits")));
  await assertFails(setDoc(doc(db("bob"), "config", "limits"), { signalsPerHour: 9999 }));
  await assertFails(getDocs(collection(db("bob"), "config")));
});

test("questions: only my own answer, locked once both answered", async () => {
  await pairAB();
  await assertSucceeds(answer("alice"));
  await assertSucceeds(answer("alice", "2026-11-26", "Değiştirdim"));   // still editable
  // Bob cannot touch Alice's answer.
  await assertFails(setDoc(sub("bob", "questions", "2026-11-26"), {
    question: "q", answers: { alice: { name: "Bob", text: "fake", at: 1 } }, updatedAt: serverTimestamp(),
  }, { merge: true }));
  await assertFails(updateDoc(sub("bob", "questions", "2026-11-26"), { "answers.alice": deleteField(), updatedAt: serverTimestamp() }));
  await assertSucceeds(answer("bob"));
  // Both answered: locked for both.
  await assertFails(answer("alice", "2026-11-26", "late change"));
  await assertFails(answer("bob", "2026-11-26", "late change"));
  await assertFails(answer("alice", "not-a-day"));
  await assertFails(answer("alice", "2026-11-27", "x".repeat(501)));
});

test("notes: create, edit, pin; author is fixed and edits are stamped", async () => {
  await pairAB();
  const ref = sub("alice", "notes", "n1");
  await assertSucceeds(setDoc(ref, note("alice", "Alice")));
  await assertFails(setDoc(sub("alice", "notes", "n2"), note("bob", "Bob")));          // forged author
  await assertFails(setDoc(sub("alice", "notes", "n3"), note("alice", "Alice", { text: "x".repeat(1001) })));
  await assertFails(setDoc(sub("alice", "notes", "n4"), note("alice", "Alice", { color: 9 })));
  // Bob edits it the way NoteRepository.update does.
  await assertSucceeds(updateDoc(sub("bob", "notes", "n1"), { text: "edited", color: 1, updatedAt: Date.now(), updatedBy: "bob" }));
  await assertFails(updateDoc(sub("bob", "notes", "n1"), { text: "edited", updatedAt: Date.now(), updatedBy: "alice" }));
  await assertFails(updateDoc(sub("bob", "notes", "n1"), { text: "no stamp" }));
  await assertFails(updateDoc(sub("bob", "notes", "n1"), { by: "bob" }));
  await assertSucceeds(updateDoc(sub("bob", "notes", "n1"), { pinned: true }));
});

test("plans, dreams, capsules and wheels accept the app's writes only", async () => {
  await pairAB();
  const now = Date.now();
  const event = { title: "Yıldönümü", date: "2027-02-14", emoji: "💛", yearly: true, by: "alice", byName: "Alice", at: now };
  await assertSucceeds(setDoc(sub("alice", "events", "e1"), event));
  await assertSucceeds(updateDoc(sub("bob", "events", "e1"), { title: "Yıl dönümü", date: "2027-02-14", emoji: "💛", yearly: true, updatedAt: now, updatedBy: "bob" }));
  await assertFails(setDoc(sub("alice", "events", "e2"), { ...event, date: "14.02.2027" }));
  await assertFails(setDoc(sub("alice", "events", "e3"), { ...event, title: "x".repeat(61) }));

  const dream = { title: "Japonya", category: "seyahat", goal: "2027", by: "bob", byName: "Bob", at: now, doneAt: null };
  await assertSucceeds(setDoc(sub("bob", "dreams", "d1"), dream));
  await assertSucceeds(updateDoc(sub("alice", "dreams", "d1"), { doneAt: now }));        // DreamRepository.setDone
  await assertFails(updateDoc(sub("alice", "dreams", "d1"), { title: "Kore" }));          // content edit without stamp
  await assertFails(setDoc(sub("bob", "dreams", "d2"), { ...dream, category: "x" }));

  const capsule = { title: "Bir yıl sonra", text: "Merhaba", openOn: "2027-11-26", by: "alice", byName: "Alice", at: now };
  await assertSucceeds(setDoc(sub("alice", "capsules", "c1"), capsule));
  await assertFails(updateDoc(sub("alice", "capsules", "c1"), { openOn: "2026-01-01" }));  // never edited
  await assertFails(setDoc(sub("alice", "capsules", "c2"), { ...capsule, text: "x".repeat(4001) }));

  await assertSucceeds(setDoc(sub("alice", "wheels", "food"), { label: "Ne yesek?", items: ["Pizza", "Sushi"], at: now }));
  await assertFails(setDoc(sub("alice", "wheels", "w2"), { label: "Çok", items: Array(13).fill("a"), at: now }));
  await assertFails(setDoc(sub("alice", "wheels", "w3"), { label: "Uzun", items: ["a", "x".repeat(21)], at: now }));
  const spin = { label: "Ne yesek?", result: "Pizza", by: "alice", byName: "Alice", at: now };
  await assertSucceeds(setDoc(sub("alice", "wheelSpins", "food"), spin));
  await assertSucceeds(setDoc(sub("alice", "wheelLog", "l1"), { ...spin, wheelId: "food" }));
  await assertFails(setDoc(sub("alice", "wheelSpins", "food"), { ...spin, by: "bob" }));
  await assertFails(updateDoc(sub("alice", "wheelLog", "l1"), { result: "Sushi" }));
});

test("per-person docs: mood, mood log and location are only mine", async () => {
  await pairAB();
  const mood = { kind: "mutlu", note: "", at: Date.now() };
  await assertSucceeds(setDoc(sub("alice", "moods", "alice"), mood));
  await assertFails(setDoc(sub("alice", "moods", "bob"), mood));
  await assertFails(setDoc(sub("alice", "moods", "alice"), { ...mood, note: "x".repeat(141) }));
  await assertSucceeds(setDoc(sub("alice", "moodLog", "alice_2026-11-26"), { ...mood, uid: "alice", day: "2026-11-26" }));
  await assertFails(setDoc(sub("alice", "moodLog", "bob_2026-11-26"), { ...mood, uid: "bob", day: "2026-11-26" }));
  await assertFails(setDoc(sub("alice", "moodLog", "alice_2026-11-25"), { ...mood, uid: "alice", day: "2026-11-26" }));
  const loc = { city: "İstanbul", lat: 41.0, lng: 29.0, cityOnly: true, at: Date.now() };
  await assertSucceeds(setDoc(sub("alice", "locations", "alice"), loc));
  await assertFails(setDoc(sub("alice", "locations", "bob"), loc));
  await assertFails(setDoc(sub("alice", "locations", "alice"), { ...loc, lat: 200 }));
  // Unpairing: the leaving partner deletes everything, also the other's docs.
  await assertSucceeds(deleteDoc(sub("bob", "locations", "alice")));
});

test("cat, drawing and settings accept the app's writes only", async () => {
  await pairAB();
  const need = { level: 80.0, at: Timestamp.now() };
  const cat = {
    name: "Mırmır", hunger: need, thirst: need, energy: need, love: need, sleepingSince: null,
    lastAction: null, recent: [], streak: { count: 0, best: 0, lastCompleteDay: null, careDay: null, careBy: [] }, wearing: null,
  };
  await assertSucceeds(setDoc(sub("alice", "cat", "main"), cat));
  await assertSucceeds(setDoc(sub("bob", "cat", "main"), { wearing: "bow" }, { merge: true }));   // setWearing
  await assertFails(setDoc(sub("bob", "cat", "main"), { name: "x".repeat(21) }, { merge: true }));
  await assertFails(setDoc(sub("bob", "cat", "main"), { hacked: true }, { merge: true }));
  await assertFails(setDoc(sub("bob", "cat", "other"), cat));

  const stroke = { by: "alice", color: 4294936970, width: 0.022, points: [0.1, 0.2, 0.3, 0.4], at: Date.now() };
  await assertSucceeds(setDoc(sub("alice", "strokes", "s1"), stroke));
  await assertFails(setDoc(sub("alice", "strokes", "s2"), { ...stroke, by: "bob" }));
  await assertFails(setDoc(sub("alice", "strokes", "s3"), { ...stroke, points: Array(1001).fill(0.5) }));
  await assertSucceeds(setDoc(sub("alice", "drawing", "presence"), { alice: Date.now() }, { merge: true }));
  await assertSucceeds(setDoc(sub("bob", "drawing", "presence"), { bob: Date.now() }, { merge: true }));
  await assertFails(setDoc(sub("bob", "drawing", "presence"), { alice: 1 }, { merge: true }));
  await assertSucceeds(setDoc(sub("alice", "drawings", "d_s1"), { strokes: [stroke], by: "alice", byName: "Alice", at: Date.now() }));

  await assertSucceeds(setDoc(sub("alice", "settings", "main"), { zone: "Europe/Istanbul", setBy: "alice", at: serverTimestamp() }, { merge: true }));
  await assertFails(setDoc(sub("alice", "settings", "main"), { zone: "Europe/Istanbul", setBy: "bob", at: serverTimestamp() }, { merge: true }));
});

test("unknown shared collections cannot be written", async () => {
  await pairAB();
  await assertFails(setDoc(sub("alice", "junk", "x"), { a: 1 }));
  await assertFails(setDoc(sub("alice", "notes", "n1", "deeper", "x"), { a: 1 }));
});
