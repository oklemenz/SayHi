package de.oklemenz.sayhi.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import butterknife.Bind;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Enum;
import de.oklemenz.sayhi.view.RelationTypeAdapter;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class RelationTypeActivity extends BaseActivity {

    @Bind(R.id.relationTypeList)
    ListView relationTypeList;

    public String selectedCode;
    public final static String DELEGATE = "delegate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.relation_type);

        Intent intent = getIntent();
        this.selectedCode = intent.getStringExtra("selectedCode");

        relationTypeList.setAdapter(new RelationTypeAdapter(this, selectedCode));

        relationTypeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RelationTypeAdapter adapter = ((RelationTypeAdapter) parent.getAdapter());
                Enum.RelationType relationType = adapter.getRelationType(position);
                if (relationType != null) {
                    selectedCode = relationType.code;
                    adapter.selectedCode = selectedCode;
                    adapter.notifyDataSetChanged();

                    ResultReceiver receiver = getIntent().getParcelableExtra(DELEGATE);
                    Bundle resultData = new Bundle();
                    resultData.putString("code", selectedCode);
                    receiver.send(Activity.RESULT_OK, resultData);
                    finish();
                }
            }
        });
    }
}
