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
import android.widget.Toast;

import com.samourai.txtenna.prefs.PrefsUtil;
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

        Preference aboutPref = (Preference) findPreference("about");
        aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new AlertDialog.Builder(SettingsActivity.this)
                        .setIcon(R.mipmap.ic_launcher_round)
                        .setTitle(R.string.app_name)
                        .setMessage(getText(R.string.app_name) + ", " + getText(R.string.version_name))
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.muletools, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();

                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MuleTools/MuleTools"));
                                startActivity(browserIntent);
                            }
                        }).show();

                return true;
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
