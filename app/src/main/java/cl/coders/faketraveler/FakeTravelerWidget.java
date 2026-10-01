package cl.coders.faketraveler;

import static cl.coders.faketraveler.MainActivity.DECIMAL_FORMAT;
import static cl.coders.faketraveler.MainActivity.sharedPrefKey;
import static cl.coders.faketraveler.SharedPrefsUtil.getDouble;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class FakeTravelerWidget extends AppWidgetProvider {

    public static final String ACTION_TOGGLE_MOCK = "cl.coders.faketraveler.action.TOGGLE_MOCK";
    public static final String ACTION_STOP_MOCK = "cl.coders.faketraveler.action.STOP_MOCK";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_TOGGLE_MOCK.equals(action)) {
                if (MockedLocationService.isMocking()) {
                    MockedLocationService.stopMockService(context);
                } else {
                    MockedLocationService.startMockService(context);
                }
                updateAllWidgets(context);
            } else if (ACTION_STOP_MOCK.equals(action)) {
                MockedLocationService.stopMockService(context);
                updateAllWidgets(context);
            }
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences sharedPref = context.getSharedPreferences(sharedPrefKey, Context.MODE_PRIVATE);
        double lat = getDouble(sharedPref, "lat", 12.0);
        double lng = getDouble(sharedPref, "lng", 15.0);
        boolean isMocking = MockedLocationService.isMocking();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_fake_traveler);

        // Coordinates display
        String coordsText = DECIMAL_FORMAT.format(lat) + ", " + DECIMAL_FORMAT.format(lng);
        views.setTextViewText(R.id.widget_coordinates, coordsText);

        // Status & button state
        if (isMocking) {
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_status_active));
            views.setTextColor(R.id.widget_status, ContextCompat.getColor(context, R.color.widget_status_active));
            views.setTextViewText(R.id.widget_button_toggle, context.getString(R.string.widget_action_stop));
            views.setTextColor(R.id.widget_button_toggle, ContextCompat.getColor(context, R.color.widget_button_stop_text));
            views.setInt(R.id.widget_button_toggle, "setBackgroundResource", R.drawable.widget_button_stop);
        } else {
            views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_status_inactive));
            views.setTextColor(R.id.widget_status, ContextCompat.getColor(context, R.color.widget_status_inactive));
            views.setTextViewText(R.id.widget_button_toggle, context.getString(R.string.widget_action_start));
            views.setTextColor(R.id.widget_button_toggle, ContextCompat.getColor(context, R.color.widget_button_start_text));
            views.setInt(R.id.widget_button_toggle, "setBackgroundResource", R.drawable.widget_button_start);
        }

        // Tap on info / icon opens MainActivity
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingLaunch = PendingIntent.getActivity(context, 0, launchIntent, getPendingIntentFlags());
        views.setOnClickPendingIntent(R.id.widget_icon, pendingLaunch);
        views.setOnClickPendingIntent(R.id.widget_info_container, pendingLaunch);

        // Tap on toggle button toggles mock state
        Intent toggleIntent = new Intent(context, FakeTravelerWidget.class);
        toggleIntent.setAction(ACTION_TOGGLE_MOCK);
        PendingIntent pendingToggle = PendingIntent.getBroadcast(context, appWidgetId, toggleIntent, getPendingIntentFlags());
        views.setOnClickPendingIntent(R.id.widget_button_toggle, pendingToggle);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static void updateAllWidgets(@NonNull Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, FakeTravelerWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private static int getPendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.FLAG_UPDATE_CURRENT;
    }
}
