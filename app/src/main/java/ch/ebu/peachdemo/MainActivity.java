package ch.ebu.peachdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import ch.ebu.peachcollector.Event;
import ch.ebu.peachcollector.PeachCollector;
import ch.ebu.peachcollector.Publisher;
import ch.ebu.peachcollector.EventStatus;

import static ch.ebu.peachcollector.Constant.*;

public class MainActivity extends AppCompatActivity {

    private static final String DEFAULT_PUBLISHER = "Default Publisher";
    private static final String MEDIA_PUBLISHER = "Media Publisher";

    private TextView logTextView;
    LogReceiver receiver = new LogReceiver();


    private TextView publisher1Title;
    private TextView publisher1Config;
    private TextView publisher1Count;
    private TextView publisher2Title;
    private TextView publisher2Config;
    private TextView publisher2Count;

    public class LogReceiver extends BroadcastReceiver {
        public LogReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String previousText = logTextView.getText().toString();
            String finalText = intent.getStringExtra(PEACH_LOG_NOTIFICATION_MESSAGE) + "\n" + previousText;
            logTextView.setText(finalText);

            PeachCollector collector = PeachCollector.init(getApplication());
            for (String publisherName: collector.publishers.keySet()) {
                List<EventStatus> statuses = collector.database.peachCollectorEventDao().getPendingStatuses(publisherName);
                if (publisherName.equalsIgnoreCase(DEFAULT_PUBLISHER)) {
                    publisher1Count.setText("" + statuses.size());
                }
                else publisher2Count.setText("" + statuses.size());
            }


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);
        logTextView.setMovementMethod(new ScrollingMovementMethod());

        publisher1Title = findViewById(R.id.publisher_title);
        publisher1Config = findViewById(R.id.publisher_config);
        publisher1Count = findViewById(R.id.publisher_count);
        publisher2Title = findViewById(R.id.publisher2_title);
        publisher2Config = findViewById(R.id.publisher2_config);
        publisher2Count = findViewById(R.id.publisher2_count);

        PeachCollector.isUnitTesting = true;
        PeachCollector.init(getApplication());

        Publisher publisher = new Publisher("zzebu00000000017");
        PeachCollector.addPublisher(publisher, DEFAULT_PUBLISHER);

        Publisher publisher2 = new Publisher("zzebu00000000017"){
            @Override
            public boolean shouldProcessEvent(Event event) {
                if (event.getType().contains("media")) return true;
                return false;
            }
        };
        publisher2.maxEventsPerBatch = 5;
        publisher2.interval = 5;
        PeachCollector.addPublisher(publisher2, MEDIA_PUBLISHER);

        String publisher1ConfigText = "MaxEvents: " + publisher.maxEventsPerBatch + " / Interval: " + publisher.interval + "\nURL: http://peach.ebu.io/collect...";
        publisher1Config.setText(publisher1ConfigText);
        publisher1Title.setText(DEFAULT_PUBLISHER);

        String publisher2ConfigText = "MaxEvents: " + publisher2.maxEventsPerBatch + " / Interval: " + publisher2.interval + "\nURL: http://peach.ebu.io/collect...";
        publisher2Config.setText(publisher2ConfigText);
        publisher2Title.setText(MEDIA_PUBLISHER);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PEACH_LOG_NOTIFICATION);
        registerReceiver(receiver, filter);

        RecommendationsFragment recommendationsFragment = new RecommendationsFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, recommendationsFragment, "recommendationsFragment").addToBackStack("recommendationsFragment").commit();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
