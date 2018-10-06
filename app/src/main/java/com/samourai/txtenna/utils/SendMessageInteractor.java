package com.samourai.txtenna.utils;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.gotenna.sdk.commands.GTCommand.GTCommandResponseListener;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.types.GTDataTypes.GTCommandResponseCode;
import com.gotenna.sdk.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An helper class that allows us to send messages via the goTenna.
 * <p>
 * Created on: 7/14/17.
 *
 * @author Thomas Colligan
 */

public class SendMessageInteractor
{
    //==============================================================================================
    // Class Properties
    //==============================================================================================

    private static final String TAG = "SendMessageInteractor";
    private static final int MESSAGE_RESEND_DELAY_MILLISECONDS = 5000;
    private final GTCommandCenter gtCommandCenter;
    private final Handler messageResendHandler;
    private final List<SendMessageItem> messageQueue;
    private boolean isSending;

    //==============================================================================================
    // Constructor
    //==============================================================================================

    public SendMessageInteractor()
    {
        gtCommandCenter = GTCommandCenter.getInstance();
        messageResendHandler = new Handler(Looper.getMainLooper());
        messageQueue = new ArrayList<>();
    }

    //==============================================================================================
    // Class Instance Methods
    //==============================================================================================

    public void sendBroadcastMessage(@NonNull final Message message, @NonNull final SendMessageListener sendMessageListener)
    {
        SendMessageItem sendMessageItem = new SendMessageItem();
        sendMessageItem.message = message;
        sendMessageItem.sendMessageListener = sendMessageListener;
        sendMessageItem.isBroadcast = true;

        messageQueue.add(sendMessageItem);
        attemptToSendMessage();

    }
/*
    public void sendMessage(@NonNull final Message message, final boolean willEncrypt, @NonNull final SendMessageListener sendMessageListener)
    {
        SendMessageItem sendMessageItem = new SendMessageItem();
        sendMessageItem.message = message;
        sendMessageItem.willEncrypt = willEncrypt;
        sendMessageItem.sendMessageListener = sendMessageListener;
        sendMessageItem.isBroadcast = false;

        messageQueue.add(sendMessageItem);
        attemptToSendMessage();
    }
*/
    private void attemptToSendMessage()
    {
        if (!isSending && !messageQueue.isEmpty())
        {
            messageResendHandler.removeCallbacks(attemptToSendRunnable);
            isSending = true;
            SendMessageItem sendMessageItem = messageQueue.get(0);

            if (sendMessageItem.isBroadcast)
            {
                sendBroadcast(sendMessageItem);
            }
            else
            {
                sendMessage(sendMessageItem);
            }
        }
    }

    private void markMessageAsSentAndSendNext(SendMessageItem sendMessageItem)
    {
        messageQueue.remove(sendMessageItem);
        isSending = false;
        attemptToSendMessage();
    }

    private void sendBroadcast(final SendMessageItem sendMessageItem)
    {
        gtCommandCenter.sendBroadcastMessage(sendMessageItem.message.toBytes(), new GTCommandResponseListener()
        {
            @Override
            public void onResponse(GTResponse response)
            {
                // All broadcasts only travel 1 hop max, so you can ignore this number
                int hopCount = response.getHopCount();
                sendMessageItem.message.setHopCount(hopCount);

                if (response.getResponseCode() == GTCommandResponseCode.POSITIVE)
                {
                    sendMessageItem.message.setMessageStatus(Message.MessageStatus.SENT_SUCCESSFULLY);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                }
                else
                {
                    sendMessageItem.message.setMessageStatus(Message.MessageStatus.ERROR_SENDING);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                }

                markMessageAsSentAndSendNext(sendMessageItem);
            }
        }, new GTErrorListener()
        {
            @Override
            public void onError(GTError error)
            {
                if (error.getCode() == GTError.DATA_RATE_LIMIT_EXCEEDED)
                {
                    Log.w(TAG, String.format(Locale.US, "Data rate limit was exceeded. Resending message in %d seconds", MESSAGE_RESEND_DELAY_MILLISECONDS / Utils.MILLISECONDS_PER_SECOND));
                    attemptToResendWithDelay();
                }
                else
                {
                    Log.w(TAG, error.toString());
                    sendMessageItem.message.setMessageStatus(Message.MessageStatus.ERROR_SENDING);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();

                    markMessageAsSentAndSendNext(sendMessageItem);
                }
            }
        });
    }

    private void sendMessage(final SendMessageItem sendMessageItem)
    {
        gtCommandCenter.sendMessage(sendMessageItem.message.toBytes(), sendMessageItem.message.getReceiverGID(), new GTCommandResponseListener()
        {
            @Override
            public void onResponse(GTResponse response)
            {
                // For goTenna Mesh, responses will have a hop count to indicate how many hops the unit took to reach its destination
                // A direct A -> B transaction is 1 hop, A -> B -> C is 2 hops, etc...
                // If you are using the V1 goTenna, the hop count will always be 0.
                // The goTenna Mesh only supports up to 3 hops currently.
                int hopCount = response.getHopCount();
                sendMessageItem.message.setHopCount(hopCount);

                if (response.getResponseCode() == GTCommandResponseCode.POSITIVE)
                {
                    sendMessageItem.message.setMessageStatus(Message.MessageStatus.SENT_SUCCESSFULLY);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                }
                else
                {
                    sendMessageItem.message.setMessageStatus(Message.MessageStatus.ERROR_SENDING);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                }

                markMessageAsSentAndSendNext(sendMessageItem);
            }
        }, new GTErrorListener()
        {
            @Override
            public void onError(GTError error)
            {
                if (error.getCode() == GTError.DATA_RATE_LIMIT_EXCEEDED)
                {
                    Log.w(TAG, String.format(Locale.US, "Data rate limit was exceeded. Resending message in %d seconds", MESSAGE_RESEND_DELAY_MILLISECONDS / Utils.MILLISECONDS_PER_SECOND));
                    attemptToResendWithDelay();
                }
                else
                {
                    Log.w(TAG, error.toString());

                    sendMessageItem.message.setMessageStatus(Message.MessageStatus.ERROR_SENDING);
                    sendMessageItem.sendMessageListener.onMessageResponseReceived();
                    markMessageAsSentAndSendNext(sendMessageItem);
                }
            }
        }, sendMessageItem.willEncrypt);
    }

    private void attemptToResendWithDelay()
    {
        isSending = false;
        messageResendHandler.removeCallbacks(attemptToSendRunnable);
        messageResendHandler.postDelayed(attemptToSendRunnable, MESSAGE_RESEND_DELAY_MILLISECONDS);
    }

    private final Runnable attemptToSendRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            attemptToSendMessage();
        }
    };

    //==============================================================================================
    // SendMessageItem Class
    //==============================================================================================

    private class SendMessageItem
    {
        Message message;
        boolean willEncrypt;
        SendMessageListener sendMessageListener;
        boolean isBroadcast;
    }

    //==============================================================================================
    // SendMessageListener
    //==============================================================================================

    public interface SendMessageListener
    {
        void onMessageResponseReceived();
    }
}
