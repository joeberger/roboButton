package com.ndipatri.roboButton.dagger.daos;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.ndipatri.roboButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.roboButton.models.Region;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ndipatri on 5/29/14.
 */
public class RegionDAO {

    private static final String TAG = RegionDAO.class.getCanonicalName();

    private Context context;

    public RegionDAO(Context context) {
        this.context = context;
    }

    public void createOrUpdateRegion(final Region dirtyRegion) {

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Region, Long> regionDao = helper.getRegionDao();

        regionDao.createOrUpdate(dirtyRegion);

        OpenHelperManager.releaseHelper();
    }

    public Region getRegion(final Integer minor, final Integer major, final String uuid) {
        return getRegion(minor, major, uuid, false);
    }

    public Region getRegion(final Integer minor, final Integer major, final String uuid, final boolean mustBePaired) {

        Region region = null;

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Region, Long> regionDao = helper.getRegionDao();

        QueryBuilder<Region, Long> queryBuilder = regionDao.queryBuilder();
        try {
            Where<Region, Long> where = queryBuilder.where();
            where.eq(Region.MINOR_COLUMN_NAME, minor);
            where.eq(Region.MAJOR_COLUMN_NAME, major);
            where.eq(Region.UUID_COLUMN_NAME, uuid);
            if (mustBePaired) {
                where.and();
                where.isNotNull(Region.BUTTON_ID);
            }

            PreparedQuery<Region> preparedQuery = queryBuilder.prepare();
            List<Region> regions = regionDao.query(preparedQuery);
            if (regions != null && regions.size() == 1) {
                region = regions.get(0);
            }
        } catch (SQLException s) {
            Log.e(TAG, "Exception while retrieving Region for Major value '" + major + "'.");
            region = null;
        }
        OpenHelperManager.releaseHelper();

        return region;
    }

    public void delete(Region region) {
        if (region == null) {
            return;
        }

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Region, Long> regionDao = helper.getRegionDao();

        regionDao.delete(region);
        OpenHelperManager.releaseHelper();
    }
}
