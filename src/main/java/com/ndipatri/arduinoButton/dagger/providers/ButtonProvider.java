package com.ndipatri.arduinoButton.dagger.providers;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.ndipatri.arduinoButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.arduinoButton.models.Button;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ndipatri on 5/29/14.
 */
public class ButtonProvider {

    private static final String TAG = ButtonProvider.class.getCanonicalName();

    private Context context;

    public ButtonProvider(Context context) {
        this.context = context;
    }

    public void createOrUpdateButton(final Button dirtyButton) {

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Button, Long> buttonDao = helper.getButtonDao();

        buttonDao.createOrUpdate(dirtyButton);

        OpenHelperManager.releaseHelper();
    }

    public Button getButton(String buttonId) {

        Button button = null;

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Button, Long> buttonDao = helper.getButtonDao();

        QueryBuilder<Button, Long> queryBuilder = buttonDao.queryBuilder();
        try {
            Where<Button, Long> where = queryBuilder.where();
            where.eq(Button.ID_COLUMN_NAME, buttonId);
            PreparedQuery<Button> preparedQuery = queryBuilder.prepare();
            List<Button> buttons = buttonDao.query(preparedQuery);
            if (buttons != null && buttons.size() == 1) {
                button = buttons.get(0);
            }
        } catch (SQLException s) {
            Log.e(TAG, "Exception while retrieving button for buttonId '" + buttonId + "'.");
            button = null;
        }
        OpenHelperManager.releaseHelper();

        return button;
    }
}
