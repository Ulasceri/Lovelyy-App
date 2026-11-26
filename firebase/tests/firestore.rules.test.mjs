// Checks that firestore.rules allow the real app flows and block everything else.
import { test, before, after, beforeEach } from "node:test";
import { readFileSync } from "node:fs";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import {
  doc, getDoc, setDoc, updateDoc, deleteDoc, collection, getDocs,
  writeBatch, runTransaction, serverTimestamp, Timestamp,
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
  await assertSucceeds(setDoc(doc(db("alice"), "couples", CODE, "notes", "n1"), { text: "hi" }));
  await assertSucceeds(getDoc(doc(db("bob"), "couples", CODE, "notes", "n1")));
  await assertFails(getDoc(doc(db("eve"), "couples", CODE, "notes", "n1")));
  await assertFails(setDoc(doc(db("eve"), "couples", CODE, "notes", "n2"), { text: "x" }));
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

// What the leaving partner's app does: end the couple and leave it in one batch, then delete the shared docs.
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
  await assertSucceeds(setDoc(doc(db("alice"), "couples", CODE, "notes", "n1"), { text: "hi" }));
  await assertSucceeds(endAsAlice());
  // Alice already left (coupleId null) but is still a member, so she can delete the shared data.
  await assertSucceeds(deleteDoc(doc(db("alice"), "couples", CODE, "notes", "n1")));
  // Ended: nothing new can be written, deletes still work, reads still work for members.
  await assertFails(setDoc(doc(db("bob"), "couples", CODE, "notes", "n2"), { text: "late" }));
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
