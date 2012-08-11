/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.cmparts.intents;

import com.cyanogenmod.cmparts.activities.CPUActivity;
import com.cyanogenmod.cmparts.activities.PerformanceSettingsActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class CPUReceiver extends BroadcastReceiver {

    private static final String TAG = "CPUSettings";

    private static final String CPU_SETTINGS_PROP = "sys.cpufreq.restored";
    private static final String KSM_SETTINGS_PROP = "sys.ksm.restored";

    @Override
    public void onReceive(Context ctx, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                setScreenOffCPU(ctx, true);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                setScreenOffCPU(ctx, false);
        } else if (SystemProperties.getBoolean(CPU_SETTINGS_PROP, false) == false
                && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SystemProperties.set(CPU_SETTINGS_PROP, "true");
            SystemProperties.set(KSM_SETTINGS_PROP, "true");
            configureCPU(ctx);
            configureKSM(ctx);
            configureSDCARD(ctx);
            configureLOWMEMKILL(ctx);
        } else {
            SystemProperties.set(KSM_SETTINGS_PROP, "false");
            SystemProperties.set(CPU_SETTINGS_PROP, "false");
        }

    }

    private void setScreenOffCPU(Context ctx, boolean screenOff) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String maxFrequency = prefs.getString(CPUActivity.MAX_FREQ_PREF, null);
        String maxSoFrequency = prefs.getString(CPUActivity.SO_MAX_FREQ_PREF, null);
        if (maxSoFrequency == null || maxFrequency == null) {
            Log.i(TAG, "Screen off or normal max CPU freq not saved. No change.");
        } else {
            if (screenOff) {
                CPUActivity.writeOneLine(CPUActivity.FREQ_MAX_FILE, maxSoFrequency);
                Log.i(TAG, "Screen off max CPU freq set");
            } else {
                CPUActivity.writeOneLine(CPUActivity.FREQ_MAX_FILE, maxFrequency);
                Log.i(TAG, "Normal (screen on) max CPU freq restored");
            }
        }
    }

    private void configureSDCARD(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    
        CPUActivity.writeOneLine(PerformanceSettingsActivity.SDCARD_RUN_FILE, prefs.getString(PerformanceSettingsActivity.SDCARD_PREF,
                      PerformanceSettingsActivity.SDCARD_PREF_DEFAULT));
        Log.d(TAG, "SdCard settings restored.");
    }

    private void configureLOWMEMKILL(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    
        CPUActivity.writeOneLine(PerformanceSettingsActivity.LOWMEMKILL_RUN_FILE, prefs.getString(PerformanceSettingsActivity.LOWMEMKILL_PREF,
                      PerformanceSettingsActivity.LOWMEMKILL_PREF_DEFAULT));
        Log.d(TAG, "LowMemKill settings restored.");
    }

    private void configureKSM(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        boolean ksm = prefs.getBoolean(PerformanceSettingsActivity.KSM_PREF, false);
        CPUActivity.writeOneLine(PerformanceSettingsActivity.KSM_SLEEP_RUN_FILE, prefs.getString(PerformanceSettingsActivity.KSM_SLEEP_PREF,
                                 PerformanceSettingsActivity.KSM_SLEEP_PREF_DEFAULT));
        CPUActivity.writeOneLine(PerformanceSettingsActivity.KSM_SCAN_RUN_FILE, prefs.getString(PerformanceSettingsActivity.KSM_SCAN_PREF,
                                 PerformanceSettingsActivity.KSM_SCAN_PREF_DEFAULT));
        CPUActivity.writeOneLine(PerformanceSettingsActivity.KSM_RUN_FILE, ksm ? "1" : "0");
        Log.d(TAG, "KSM settings restored.");
    }

    private void configureCPU(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (prefs.getBoolean(CPUActivity.SOB_PREF, false) == false) {
            Log.i(TAG, "Restore disabled by user preference.");
            return;
        }

        String governor = prefs.getString(CPUActivity.GOV_PREF, null);
        String minFrequency = prefs.getString(CPUActivity.MIN_FREQ_PREF, null);
        String maxFrequency = prefs.getString(CPUActivity.MAX_FREQ_PREF, null);
        String availableFrequenciesLine = CPUActivity.readOneLine(CPUActivity.FREQ_LIST_FILE);
        String availableGovernorsLine = CPUActivity.readOneLine(CPUActivity.GOVERNORS_LIST_FILE);
        boolean noSettings = ((availableGovernorsLine == null) || (governor == null)) && 
                             ((availableFrequenciesLine == null) || ((minFrequency == null) && (maxFrequency == null)));
        List<String> frequencies = null;
        List<String> governors = null;
        
        if (noSettings) {
            Log.d(TAG, "No settings saved. Nothing to restore.");
        } else {
            if (availableGovernorsLine != null){
                governors = Arrays.asList(availableGovernorsLine.split(" "));  
            }
            if (availableFrequenciesLine != null){
                frequencies = Arrays.asList(availableFrequenciesLine.split(" "));  
            }
            if (governor != null && governors != null && governors.contains(governor)) {
                CPUActivity.writeOneLine(CPUActivity.GOVERNOR, governor);
            }
            if (maxFrequency != null && frequencies != null && frequencies.contains(maxFrequency)) {
                CPUActivity.writeOneLine(CPUActivity.FREQ_MAX_FILE, maxFrequency);
            }
            if (minFrequency != null && frequencies != null && frequencies.contains(minFrequency)) {
                CPUActivity.writeOneLine(CPUActivity.FREQ_MIN_FILE, minFrequency);
            }
            Log.d(TAG, "CPU settings restored.");
        }
    }
}
