package org.sunbird.dao.notification.impl;

import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.notification.ScheduleNotificationDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;

import java.util.Map;

public class ScheduleNotificationDaoImpl implements ScheduleNotificationDao {

    private final LoggerUtil logger = new LoggerUtil(ScheduleNotificationDaoImpl.class);
    private static ScheduleNotificationDao scheduleNotificationDao = null;
    private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private static final String TABLE_NAME = JsonKey.SCHEDULE_NOTIFICATIONS;
    private static final String KEY_SPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_NOTIFICATIONS);



    @Override
    public Response saveScheduledNotificationDetails(Map<String, Object> request, RequestContext context) {
        return cassandraOperation.insertRecord("sunbird_notifications", TABLE_NAME, request, context);
    }


    public static ScheduleNotificationDao getInstance() {
        if (scheduleNotificationDao == null) {
            scheduleNotificationDao = new ScheduleNotificationDaoImpl();
        }
        return scheduleNotificationDao;
    }


}
