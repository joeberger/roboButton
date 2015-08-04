package com.ndipatri.roboButton.dagger.daos;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.stmt.Where;
import com.ndipatri.roboButton.RBApplication;
import com.ndipatri.roboButton.database.OrmLiteDatabaseHelper;
import com.ndipatri.roboButton.enums.ButtonState;
import com.ndipatri.roboButton.events.ButtonUpdatedEvent;
import com.ndipatri.roboButton.models.Button;
import com.ndipatri.roboButton.utils.BusProvider;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by ndipatri on 5/29/14.
 */
public class ButtonDao {

    private static final String TAG = ButtonDao.class.getCanonicalName();

    private Context context;

    @Inject BusProvider bus;

    public ButtonDao(Context context) {
        this.context = context;

        RBApplication.getInstance().getGraph().inject(this);

        bus.register(this);
    }

    public void createOrUpdateButton(final Button dirtyButton) {

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Button, Long> buttonDao = helper.getButtonDao();

        buttonDao.createOrUpdate(dirtyButton);

        OpenHelperManager.releaseHelper();

        bus.post(new ButtonUpdatedEvent(dirtyButton.getId()));
    }

    public void clearStateOfAllButtons() {

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Button, Long> buttonDao = helper.getButtonDao();

        UpdateBuilder<Button, Long> updateBuilder = buttonDao.updateBuilder();
        try {
            updateBuilder.updateColumnValue(Button.STATE_COLUMN_NAME, ButtonState.OFFLINE);
            updateBuilder.update();
        } catch (SQLException s) {
            Log.e(TAG, "Exception while clearing state of all buttons.");
        }
        OpenHelperManager.releaseHelper();
    }

    public List<Button> getCommunicatingButtons() {
        return getButtons(null, true);
    }

    public Button getButton(final String buttonId) {
        Button button = null;
        List<Button> buttons = getButtons(Arrays.asList(new String[] {buttonId}), false);
        if (buttons != null && !buttons.isEmpty()) {
            button = buttons.get(0);
        }

        return button;
    }


    private List<Button> getButtons(final List<String> buttonIdList, final boolean connectedOnly) {

        List<Button> buttons;

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Button, Long> buttonDao = helper.getButtonDao();

        QueryBuilder<Button, Long> queryBuilder = buttonDao.queryBuilder();
        try {
            Where<Button, Long> where = queryBuilder.where();

            if (buttonIdList != null && !buttonIdList.isEmpty()) {
                where.in(Button.ID_COLUMN_NAME, buttonIdList);
            }

            if (connectedOnly) {
                if (buttonIdList != null && !buttonIdList.isEmpty()) {
                    where.and();
                }

                where.or(where.eq(Button.STATE_COLUMN_NAME, ButtonState.OFF),
                         where.eq(Button.STATE_COLUMN_NAME, ButtonState.ON),
                         where.eq(Button.STATE_COLUMN_NAME, ButtonState.ON_PENDING),
                         where.eq(Button.STATE_COLUMN_NAME, ButtonState.OFF_PENDING));
            }

            PreparedQuery<Button> preparedQuery = queryBuilder.prepare();
            buttons = buttonDao.query(preparedQuery);
        } catch (SQLException s) {
            Log.e(TAG, "Exception while retrieving button.", s);
            buttons = null;
        }
        OpenHelperManager.releaseHelper();

        return buttons;
    }

    public void delete(Button button) {
        if (button == null) {
            return;
        }

        OrmLiteDatabaseHelper helper = OpenHelperManager.getHelper(context, OrmLiteDatabaseHelper.class);
        RuntimeExceptionDao<Button, Long> buttonDao = helper.getButtonDao();

        buttonDao.delete(button);
        OpenHelperManager.releaseHelper();
    }
}
