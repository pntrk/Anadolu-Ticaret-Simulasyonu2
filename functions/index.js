const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendPushNotification = functions.firestore
    .document('global_players/{playerId}/push_notifications/{notificationId}')
    .onCreate(async (snap, context) => {
        const notificationData = snap.data();
        const playerId = context.params.playerId;

        try {
            // Get the user's FCM token from their profile document
            const userDoc = await admin.firestore().collection('global_players').doc(playerId).get();
            if (!userDoc.exists) {
                console.log(`User ${playerId} not found.`);
                return null;
            }

            const userData = userDoc.data();
            const fcmToken = userData.fcmToken;

            if (!fcmToken) {
                console.log(`No FCM token for user ${playerId}`);
                return null;
            }

            // Create the FCM payload
            const payload = {
                notification: {
                    title: notificationData.title || "Anadolu Ticaret",
                    body: notificationData.message || notificationData.body || "Yeni bir bildiriminiz var!"
                },
                token: fcmToken
            };

            // Send the push notification
            const response = await admin.messaging().send(payload);
            console.log('Successfully sent message:', response);
            return response;
        } catch (error) {
            console.error('Error sending push notification:', error);
            return null;
        }
    });
