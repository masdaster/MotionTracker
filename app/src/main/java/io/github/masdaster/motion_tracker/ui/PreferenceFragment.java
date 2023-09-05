package io.github.masdaster.motion_tracker.ui;

import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.logging.Logger;

import io.github.masdaster.motion_tracker.R;

/**
 * Created by Z-Byte on .
 */
public class PreferenceFragment extends PreferenceFragmentCompat {
    private static final Logger logger = Logger.getLogger("PreferenceFragment");

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        setSummaryProviderOfPreference(EditTextPreference.SimpleSummaryProvider.getInstance(), "server_ip", "server_port");
        setSummaryProviderOfPreference(ListPreference.SimpleSummaryProvider.getInstance(), "sensor_type", "sensor_sample_rate");

        EditTextPreference serverPortPreference = findPreference("server_port");
        if (serverPortPreference != null) {
            serverPortPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        }
    }

    private void setSummaryProviderOfPreference(Preference.SummaryProvider<?> summaryProvider, String... keys) {
        for (String key : keys) {
            Preference preference = findPreference(key);
            if (preference == null) {
                logger.warning("Cannot find preference with key '" + key + "'.");
            } else {
                preference.setSummaryProvider(summaryProvider);
            }
        }
    }
}
