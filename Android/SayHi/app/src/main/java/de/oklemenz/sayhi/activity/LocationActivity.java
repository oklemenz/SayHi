package de.oklemenz.sayhi.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.Bind;
import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Match;
import de.oklemenz.sayhi.model.UserData;
import de.oklemenz.sayhi.service.Utilities;

/**
 * Created by Oliver Klemenz on 15.02.17.
 */

public class LocationActivity extends BaseActivity implements OnMapReadyCallback {

    @Bind(R.id.titleLabel)
    TextView titleLabel;

    private GoogleMap map;

    private int matchIndex = 0;
    private Match match;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location);

        Intent intent = getIntent();
        matchIndex = intent.getIntExtra("match", 0);
        match = UserData.getInstance().getMatch(matchIndex);

        updateTitle();
        updateContent();
    }

    public String backLabel() {
        return this.getString(R.string.Match);
    }

    private void updateTitle() {
        String titleHTML = "<b><font color='" + AppDelegate.AccentColor + "'>" + match.locationName + "</font></b>";
        String subTitle = "";
        String separator = "";
        if (!TextUtils.isEmpty(match.locationStreet)) {
            subTitle += separator + match.locationStreet;
            separator = ", ";
        }
        if (!TextUtils.isEmpty(match.locationCity)) {
            subTitle += separator + match.locationCity;
            separator = ", ";
        }
        if (!TextUtils.isEmpty(match.locationCountry)) {
            subTitle += separator + match.locationCountry;
            separator = ", ";
        }
        if (!TextUtils.isEmpty(subTitle)) {
            titleHTML += "<br/><font color='" + Color.BLACK + "'><small>" + subTitle + "</font></small>";
        }
        titleLabel.setText(Utilities.toSpan(titleHTML));
    }

    private void updateContent() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        setViewport(false);
    }

    public void setViewport(boolean animated) {
        if (map != null) {
            if (match != null) {
                LatLng location = new LatLng(Double.parseDouble(match.locationLatitude), Double.parseDouble(match.locationLongitude));
                Marker marker = map.addMarker(new MarkerOptions().position(location).title(match.locationName));
                marker.showInfoWindow();
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 17.0f);
                if (animated) {
                    map.animateCamera(cameraUpdate);
                } else {
                    map.moveCamera(cameraUpdate);
                }
            }
        }
    }

    public void onPlacemarkerPressed(View view) {
        setViewport(true);
    }
}
