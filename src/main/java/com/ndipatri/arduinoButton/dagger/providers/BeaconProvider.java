package com.ndipatri.arduinoButton.dagger.providers;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.arduinoButton.models.Beacon;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ndipatri on 5/29/14.
 */
public class BeaconProvider {

    private static final String TAG = BeaconProvider.class.getCanonicalName();

    private Context context;

    public BeaconProvider(Context context) {
        this.context = context;
    }

    public void createOrUpdateBeacon(final Beacon dirtyBeacon) {

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Beacon, Long> beaconDao = helper.getBeaconDao();

        beaconDao.createOrUpdate(dirtyBeacon);

        OpenHelperManager.releaseHelper();
    }

    public Beacon getBeacon(String macAddress) {

        Beacon beacon = null;

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Beacon, Long> beaconDao = helper.getBeaconDao();

        QueryBuilder<Beacon, Long> queryBuilder = beaconDao.queryBuilder();
        try {
            Where<Beacon, Long> where = queryBuilder.where();
            where.eq(Beacon.MAC_ADDRESS_COLUMN_NAME, macAddress);
            PreparedQuery<Beacon> preparedQuery = queryBuilder.prepare();
            List<Beacon> beacons = beaconDao.query(preparedQuery);
            if (beacons != null && beacons.size() == 1) {
                beacon = beacons.get(0);
            }
        } catch (SQLException s) {
            Log.e(TAG, "Exception while retrieving beacon for macAddress '" + macAddress + "'.");
            beacon = null;
        }
        OpenHelperManager.releaseHelper();

        return beacon;
    }
}
