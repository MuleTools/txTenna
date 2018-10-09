package com.samourai.txtenna;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import com.gotenna.sdk.GoTenna;
import com.gotenna.sdk.bluetooth.GTConnectionManager;
import com.gotenna.sdk.commands.GTCommand;
import com.gotenna.sdk.commands.GTCommandCenter;
import com.gotenna.sdk.commands.GTError;
import com.gotenna.sdk.commands.Place;
import com.gotenna.sdk.interfaces.GTErrorListener;
import com.gotenna.sdk.responses.GTResponse;
import com.gotenna.sdk.types.GTDataTypes;
import com.samourai.txtenna.prefs.PrefsUtil;
import com.samourai.txtenna.utils.IncomingMessagesManager;
import com.samourai.txtenna.utils.goTennaUtil;

import java.security.SecureRandom;
import java.util.UUID;
//import android.util.Log;

public class SettingsActivity extends PreferenceActivity {

    private static final String regex_url = "^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final EditTextPreference txTennaPref = (EditTextPreference) findPreference("txTenna");
        txTennaPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                String txTenna = newValue.toString();
                if (txTenna != null && txTenna.length() > 0 && txTenna.matches(regex_url)) {
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.TXTENNA, txTenna);
                }
                else {
                    Toast.makeText(SettingsActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });

        final EditTextPreference smsRelayPref = (EditTextPreference) findPreference("smsRelay");
        smsRelayPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                String telno = newValue.toString();
                if (telno != null && telno.length() > 0) {
                    String s = telno.replaceAll("[^\\+0-9]", "");
                    if (s.matches("^\\+[0-9]+$")) {
                        PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.SMS_RELAY, telno);
                    }
                    else {
                        Toast.makeText(SettingsActivity.this, R.string.use_intl_format, Toast.LENGTH_SHORT).show();
                    }
                }

                return true;
            }
        });
/*
        final EditTextPreference pushTxPref = (EditTextPreference) findPreference("pushTx");
        pushTxPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                String pushTx = newValue.toString();
                if (pushTx != null && pushTx.length() > 0 && pushTx.matches(regex_url)) {
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.PUSHTX, pushTx);
                }
                else {
                    Toast.makeText(SettingsActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });
*/
        final CheckBoxPreference cbMainNet = (CheckBoxPreference) findPreference("mainNet");
        cbMainNet.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (cbMainNet.isChecked()) {
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.USE_MAINNET, false);
                }
                else {
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.USE_MAINNET, true);
                }

                return true;
            }
        });

        final CheckBoxPreference cbZ85 = (CheckBoxPreference) findPreference("z85");
        cbZ85.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (cbZ85.isChecked()) {
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.USE_Z85, false);
                }
                else {
                    PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.USE_Z85, true);
                }

                return true;
            }
        });

        Preference regionPref = (Preference) findPreference("region");
        regionPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                doRegion();

                return true;
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void doRegion()	{

        final CharSequence[] regions = {
                "North America",
                "Europe",
                "Australia",
                "New Zealand",
                "Singapore",
        };

        final int sel = PrefsUtil.getInstance(SettingsActivity.this).getValue(PrefsUtil.REGION, 0);
        final int _sel;
        if(sel >= regions.length)    {
            _sel = 0;
        }
        else    {
            _sel = sel;
        }

        new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(R.string.options_region)
                .setSingleChoiceItems(regions, _sel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                PrefsUtil.getInstance(SettingsActivity.this).setValue(PrefsUtil.REGION, which);

                                if(GoTenna.tokenIsVerified() && goTennaUtil.getInstance(SettingsActivity.this).isPaired())    {

                                    Place place = null;
                                    switch(sel)    {
                                        case 1:
                                            place = Place.EUROPE;
                                            break;
                                        case 2:
                                            place = Place.AUSTRALIA;
                                            break;
                                        case 3:
                                            place = Place.NEW_ZEALAND;
                                            break;
                                        case 4:
                                            place = Place.SINGAPORE;
                                            break;
                                        default:
                                            place = Place.NORTH_AMERICA;
                                            break;
                                    }

                                    GTCommandCenter.getInstance().sendSetGeoRegion(place, new GTCommand.GTCommandResponseListener()
                                    {
                                        @Override
                                        public void onResponse(GTResponse response)
                                        {
                                            if (response.getResponseCode() == GTDataTypes.GTCommandResponseCode.POSITIVE)
                                            {
                                                Log.d("SettingsActivity", "Region set OK");
                                            }
                                            else
                                            {
                                                Log.d("SettingsActivity", "Region not set:" + response.toString());
                                            }
                                        }
                                    }, new GTErrorListener()
                                    {
                                        @Override
                                        public void onError(GTError error)
                                        {
                                            Log.d("SettingsActivity", error.toString() + "," + error.getCode());
                                        }
                                    });

                                }

                                dialog.dismiss();
                            }
                        }
                ).show();

    }

}
