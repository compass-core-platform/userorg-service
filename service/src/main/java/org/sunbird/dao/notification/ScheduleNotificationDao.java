package org.sunbird.dao.notification;

import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;

import java.util.Map;

public interface ScheduleNotificationDao {

    public Response saveScheduledNotificationDetails(Map<String, Object> request, RequestContext context);
}
