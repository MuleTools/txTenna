package com.samourai.txtenna.utils;

import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.messages.GTBaseMessageData;
import com.gotenna.sdk.messages.GTMessageData;
import com.gotenna.sdk.messages.GTTextOnlyMessageData;

import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

/**
 * A singleton that manages listening for incoming messages from the SDK and parses them into
 * usable data classes.
 * <p>
 * Created on 2/10/16
 *
 * @author ThomasColligan
 */
public class IncomingMessagesManager implements GTCommandCenter.GTMessageListener
{
    //==============================================================================================
    // Class Properties
    //==============================================================================================

    private static ArrayList<IncomingMessageListener> incomingMessageListeners = null;

    private static IncomingMessagesManager instance = null;

    private static Context context = null;

    //==============================================================================================
    // Singleton Methods
    //==============================================================================================

    private IncomingMessagesManager()
    {
        ;
    }
/*
    private static class SingletonHelper
    {
        private static final IncomingMessagesManager INSTANCE = new IncomingMessagesManager();
    }
*/
    public static IncomingMessagesManager getInstance(Context ctx)
    {
        context = ctx;

        if (instance == null) {
            incomingMessageListeners = new ArrayList<>();
            instance = new IncomingMessagesManager();
        }

        return instance;
    }

    //==============================================================================================
    // Class Instance Methods
    //==============================================================================================

    public void startListening()
    {
        GTCommandCenter.getInstance().setMessageListener(this);
    }

    public void addIncomingMessageListener(IncomingMessageListener incomingMessageListener)
    {
        synchronized (incomingMessageListeners)
        {
            if (incomingMessageListener != null)
            {
                incomingMessageListeners.remove(incomingMessageListener);
                if (incomingMessageListeners.size() > 0) {
                    Log.d("IncomingMessagesManager", "addIncomingMessageListener, NOT empty " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
                }
                incomingMessageListeners.add(incomingMessageListener);
            }
            else {
                Log.d("IncomingMessagesManager", "addIncomingMessageListener, incomingMessageListener == null  " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
            }
        }
    }

    public void removeIncomingMessageListener(IncomingMessageListener incomingMessageListener)
    {
        synchronized (incomingMessageListeners)
        {
            if (incomingMessageListener != null)
            {
                incomingMessageListeners.remove(incomingMessageListener);
            }
            else {
                Log.d("IncomingMessagesManager", "removeIncomingMessageListener, incomingMessageListener == null " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
            }
            if (incomingMessageListeners.size() > 0) {
                Log.d("IncomingMessagesManager", "removeIncomingMessageListener, NOT empty " + Integer.toHexString(this.hashCode()) + " [lifecycle]");
            }
        }
    }

    private void notifyIncomingMessage(final Message incomingMessage)
    {
        synchronized (incomingMessageListeners)
        {
            for (IncomingMessageListener incomingMessageListener : incomingMessageListeners)
            {
                incomingMessageListener.onIncomingMessage(incomingMessage);
            }
        }
    }

    //==============================================================================================
    // GTMessageListener Implementation
    //==============================================================================================

    @Override
    public void onIncomingMessage(GTMessageData messageData)
    {
        // We do not send any custom formatted messages in this app,
        // but if you wanted to send out messages with your own format, this is where
        // you would receive those messages.

        Log.d("IncomingMessagesManager", "GTMessageData:" + Hex.toHexString(messageData.getDataToProcess()));
    }

    @Override
    public void onIncomingMessage(GTBaseMessageData gtBaseMessageData)
    {
        if (gtBaseMessageData instanceof GTTextOnlyMessageData)
        {
            // Somebody sent us a message, try to parse it
            GTTextOnlyMessageData gtTextOnlyMessageData = (GTTextOnlyMessageData) gtBaseMessageData;
            Message incomingMessage = Message.createMessageFromData(gtTextOnlyMessageData);
            Log.d("IncomingMessagesManager", "GTBaseMessageData:" + incomingMessage.getText());
            notifyIncomingMessage(incomingMessage);
        }

    }

    //==============================================================================================
    // IncomingMessageListener Interface
    //==============================================================================================

    public interface IncomingMessageListener
    {
        void onIncomingMessage(Message incomingMessage);
    }
}
