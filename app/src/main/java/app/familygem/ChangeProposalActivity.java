package app.familygem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.familygem.action.GetOpenPRTask;
import com.familygem.action.GetTreeJsonInPRTask;
import com.familygem.restapi.models.Pull;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChangeProposalActivity extends AppCompatActivity {
    int treeId;
    String repoFullName;
    ListView listView;
    List<Map<String, String>> pullList;
    SimpleAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_proposal);
        treeId = getIntent().getIntExtra("treeId", 0);
        repoFullName = getIntent().getStringExtra("repoFullName");
        listView = findViewById(R.id.list);
        GetOpenPRTask.execute(ChangeProposalActivity.this, repoFullName, treeId, pulls -> {
            pullList = new ArrayList<>();
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG, Locale.getDefault());
            for (Pull pull : pulls ) {
                Map<String, String> dato = new HashMap<>(3);
                dato.put("pullNo", String.valueOf(pull.number));
                if (pull.user.name != null)
                    dato.put("proposer", pull.user.login + "(" + pull.user.name + ")");
                else
                    dato.put("proposer", pull.user.login);
                // convert to UTC and then convert to local date & time
                DateTime dateTime = ISODateTimeFormat.dateTimeParser().parseDateTime(pull.createdAt);
                dato.put("datetime", dateTime.toString(DateTimeFormat.mediumDateTime()));
                pullList.add(dato);
            }
            adapter = new SimpleAdapter(ChangeProposalActivity.this, this.pullList,
                    R.layout.proposed_change_view,
                    new String[] {"proposer", "datetime"},
                    new int[] {R.id.proposer, R.id.date_time}) {
                @Override
                public View getView(final int posiz, View convertView, ViewGroup parent) {
                    View vistaAlbero = super.getView( posiz, convertView, parent );
                    vistaAlbero.setOnClickListener(v -> {
                        int pullNo = Integer.parseInt(pullList.get(posiz).get("pullNo"));
                        showReviewChanges(pullNo);
                    });
                    return vistaAlbero;
                }
            };
            listView.setAdapter(adapter);
            // adapter.notifyDataSetChanged();
            findViewById(R.id.progress_circular).setVisibility(View.GONE);
        },
                error -> new AlertDialog.Builder(ChangeProposalActivity.this)
                        .setTitle(R.string.find_errors)
                        .setMessage(error)
                        .setCancelable(false)
                        .setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
                        .show());
    }

    private void showReviewChanges(int pullNo) {
        findViewById(R.id.progress_circular).setVisibility(View.VISIBLE);
        GetTreeJsonInPRTask.execute(ChangeProposalActivity.this, repoFullName, treeId,pullNo, (mergeable) -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ChangeProposalActivity.this)
                .setCancelable(false).setView(R.layout.review_changes_dialog)
                .setPositiveButton(R.string.approve, (dialog0, id0) -> {
                    if (isFinishing())
                        return;
                    dialog0.dismiss();

                    // TODO do approve process
                    Toast.makeText(ChangeProposalActivity.this, "TODO: approve process", Toast.LENGTH_LONG).show();
                })
                .setNeutralButton(R.string.review_changes, null);
            if (mergeable) {
                alertDialogBuilder
                .setNegativeButton(getString(R.string.reject), ((dialog0, id0) -> {
                    if (isFinishing())
                        return;

                    // TODO reject process
                    Toast.makeText(ChangeProposalActivity.this, "TODO: reject process", Toast.LENGTH_LONG).show();
                }));
            }
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            // override button
            alertDialog.findViewById(R.id.btn_close).setOnClickListener(v -> {
                alertDialog.dismiss();
            });
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                if (isFinishing())
                    return;

                Intent intent = new Intent(ChangeProposalActivity.this, CompareChangesActivity.class);
                intent.putExtra("compareType", CompareChangesActivity.CompareType.SubmitChanges);
                // before json: last time commit and head 0
                String jsonFileNameBefore = treeId + ".json";
                intent.putExtra("jsonFileNameBefore", jsonFileNameBefore);
                // after json: current json file
                String jsonFileNameAfter = treeId + ".json.PR";
                intent.putExtra("jsonFileNameAfter", jsonFileNameAfter);
                intentLauncherCompareChanges.launch(intent);
            });
            findViewById(R.id.progress_circular).setVisibility(View.GONE);
        }, error -> {
            new AlertDialog.Builder(ChangeProposalActivity.this)
                    .setTitle(R.string.find_errors)
                    .setMessage(error)
                    .setCancelable(false)
                    .setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
                    .show();
        });
    }

    private final ActivityResultLauncher<Intent> intentLauncherCompareChanges = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // do nothing --> because the action from ReviewChangesActivity is only close
                }
            });
}