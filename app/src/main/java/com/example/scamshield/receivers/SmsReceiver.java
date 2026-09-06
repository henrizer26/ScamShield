package com.example.scamshield.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import com.example.scamshield.database.DatabaseHelper;
import com.example.scamshield.utils.AppPrefs;
import com.example.scamshield.utils.NotificationHelper;
import com.example.scamshield.utils.ProtectionManager;
import com.example.scamshield.utils.ScamDetector;
import com.example.scamshield.utils.WhitelistUtils;

/** Receives new SMS events only while the user-enabled protection state is active. */
public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null
                || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())
                || !ProtectionManager.isActive(context)) {
            return;
        }

        String sender = null;
        String message = null;
        long receptionTime = System.currentTimeMillis();

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages != null) {
            StringBuilder body = new StringBuilder();
            for (SmsMessage smsMessage : messages) {
                if (sender == null) {
                    sender = smsMessage.getDisplayOriginatingAddress();
                    receptionTime = smsMessage.getTimestampMillis();
                }
                body.append(smsMessage.getMessageBody());
            }
            if (sender != null) {
                message = body.toString();
            }
        }

        if (sender == null || message == null
                || WhitelistUtils.isServiceMessage(sender, message)) {
            return;
        }

        ScamDetector.ScamResult result = ScamDetector.detect(message, sender);
        AppPrefs.incrementScannedCount(context);

        if (result.shouldAlert()) {
            new DatabaseHelper(context).addScam(
                    sender, message, result.scamType, receptionTime);
            NotificationHelper.showRiskAlert(context, sender, result);
        }
    }
}
