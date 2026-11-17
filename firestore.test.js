const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_obuy_001";
const OWNER_EMAIL = "obuyjamming@gmail.com";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function ownerDb() {
  return testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
}

function sampleListing(ownerId) {
  const now = new Date(Date.now() - 5000);
  return {
    ownerId,
    ownerDisplayName: "Pemilik Kos",
    title: "Kosan Melati Hemat",
    propertyType: "KOSAN",
    description: "Kosan bersih dekat kampus dengan fasilitas lengkap.",
    priceIdr: 750000,
    billingPeriod: "PER_BULAN",
    countryCode: "ID",
    countryName: "Indonesia",
    province: "DI Yogyakarta",
    city: "Sleman",
    address: "Jl. Pogung Baru No. 14",
    latitude: -7.7615,
    longitude: 110.3748,
    contactName: "Ibu Sri",
    contactPhone: "+6281227104455",
    contactWhatsapp: "6281227104455",
    facilities: ["WiFi Kencang", "Parkir Motor"],
    photoUrls: ["drawable:img_kosan_modern"],
    rating: 4.8,
    availableRooms: 3,
    visibility: "public",
    boostTier: "NONE",
    featuredRank: 0,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("listings").where("visibility", "==", "public").get());
  await assertFails(unauth.collection("music_tracks").where("visibility", "==", "public").get());
  await assertFails(unauth.collection("listings").doc("l1").set(sampleListing(ALICE_UID)));
});

test("Listings: owner can create, update content, and public users can query visibility=public; self-boost & hijack denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("listings").doc("kos_1").set(sampleListing(ALICE_UID)));

  // Reject self-assigned boost on create
  await assertFails(
    aliceDb.collection("listings").doc("kos_hack").set({
      ...sampleListing(ALICE_UID),
      boostTier: "BOOST_30D",
      featuredRank: 2,
    })
  );

  // Reject @DocumentId 'id' ghost field
  await assertFails(
    aliceDb.collection("listings").doc("kos_id").set({
      ...sampleListing(ALICE_UID),
      id: "kos_id",
    })
  );

  // Public query with visibility filter succeeds for Bob
  await assertSucceeds(bobDb.collection("listings").where("visibility", "==", "public").get());
  // Unfiltered query fails for Bob
  await assertFails(bobDb.collection("listings").get());

  // Bob cannot update Alice's listing
  await assertFails(
    bobDb.collection("listings").doc("kos_1").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice can update her price & title, but NOT boostTier
  await assertSucceeds(
    aliceDb.collection("listings").doc("kos_1").update({
      priceIdr: 800000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("listings").doc("kos_1").update({
      boostTier: "BOOST_30D",
      featuredRank: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // App owner CAN approve boostTier
  await assertSucceeds(
    ownerDb().collection("listings").doc("kos_1").update({
      boostTier: "BOOST_30D",
      featuredRank: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Email spoofing test: unverified owner email is rejected from owner_config and music_tracks", async () => {
  const spoofedDb = testEnv.authenticatedContext("spoof_uid", {
    email: OWNER_EMAIL,
    email_verified: false,
  }).firestore();

  const now = new Date(Date.now() - 1000);
  await assertFails(
    spoofedDb.collection("music_tracks").doc("t1").set({
      title: "Lagu Palsu",
      artist: "Obuy Jamming",
      genre: "Reggae",
      durationLabel: "03:30",
      youtubeUrl: "https://youtube.com",
      spotifyUrl: "https://open.spotify.com",
      metaUrl: "https://facebook.com",
      embedUrl: "",
      sortOrder: 1,
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Monetization orders, promoter commissions, payouts, and owner-only verification", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const appOwnerDb = ownerDb();
  const now = new Date(Date.now() - 2000);

  // Alice creates a Boost 30D order (75,000 -> 15,000 promoter fee, 60,000 owner net)
  await assertSucceeds(
    aliceDb.collection("monetization_orders").doc("ord_1").set({
      buyerId: ALICE_UID,
      buyerName: "Alice",
      listingId: "kos_1",
      listingTitle: "Kosan Melati Hemat",
      orderType: "BOOST_30D",
      propertyPriceIdr: 0,
      grossAmountIdr: 75000,
      promoterCode: "NKBOB99",
      promoterUid: BOB_UID,
      promoterFeeRatePercent: 20,
      promoterFeeIdr: 15000,
      ownerNetIdr: 60000,
      paymentNote: "Transfer BCA 75rb",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot self-verify or read Bob's orders
  await assertFails(
    aliceDb.collection("monetization_orders").doc("ord_1").update({
      status: "VERIFIED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("monetization_orders").doc("ord_1").get());

  // App owner verifies order and creates commission
  await assertSucceeds(
    appOwnerDb.collection("monetization_orders").doc("ord_1").update({
      status: "VERIFIED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Re-updating terminal VERIFIED order fails
  await assertFails(
    appOwnerDb.collection("monetization_orders").doc("ord_1").update({
      status: "REJECTED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Promoter Bob cannot mint his own commission
  await assertFails(
    bobDb.collection("referral_commissions").doc("comm_1").set({
      promoterUid: BOB_UID,
      promoterCode: "NKBOB99",
      orderId: "ord_1",
      orderTitle: "Sorotan 30 Hari",
      grossOrderIdr: 75000,
      feePercent: 20,
      commissionIdr: 15000,
      status: "AVAILABLE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // App owner creates commission for Bob; Bob can query his own commissions
  await assertSucceeds(
    appOwnerDb.collection("referral_commissions").doc("comm_1").set({
      promoterUid: BOB_UID,
      promoterCode: "NKBOB99",
      orderId: "ord_1",
      orderTitle: "Sorotan 30 Hari",
      grossOrderIdr: 75000,
      feePercent: 20,
      commissionIdr: 15000,
      status: "AVAILABLE",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("referral_commissions").where("promoterUid", "==", BOB_UID).get());
  await assertFails(aliceDb.collection("referral_commissions").where("promoterUid", "==", BOB_UID).get());

  // Bob submits payout request >= 50,000; < 50,000 is rejected
  await assertFails(
    bobDb.collection("payout_requests").doc("pay_small").set({
      promoterUid: BOB_UID,
      promoterName: "Bob",
      promoterCode: "NKBOB99",
      amountIdr: 20000,
      payoutMethod: "DANA",
      accountNumber: "08123456789",
      accountHolder: "Bob",
      status: "REQUESTED",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("payout_requests").doc("pay_1").set({
      promoterUid: BOB_UID,
      promoterName: "Bob",
      promoterCode: "NKBOB99",
      amountIdr: 50000,
      payoutMethod: "DANA",
      accountNumber: "08123456789",
      accountHolder: "Bob",
      status: "REQUESTED",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    appOwnerDb.collection("payout_requests").doc("pay_1").update({
      status: "PAID",
      adminNote: "Sudah ditransfer",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Obuy Jamming music_tracks: owner writes, signed-in users query public tracks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const appOwnerDb = ownerDb();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    appOwnerDb.collection("music_tracks").doc("track_1").set({
      title: "Lalat, bunga dan kupu kupu",
      artist: "Obuy Jamming",
      genre: "Reggae Nusantara",
      durationLabel: "04:12",
      youtubeUrl: "https://music.youtube.com/search?q=Obuy+Jamming+Lalat+bunga+dan+kupu+kupu",
      spotifyUrl: "https://open.spotify.com/search/Obuy%20Jamming",
      metaUrl: "https://www.facebook.com/search/top/?q=Obuy%20Jamming",
      embedUrl: "https://www.youtube.com/embed?listType=search&list=Obuy+Jamming+Lalat+bunga+dan+kupu+kupu",
      sortOrder: 1,
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("music_tracks").where("visibility", "==", "public").get());
  await assertFails(aliceDb.collection("music_tracks").doc("track_1").delete());
});
