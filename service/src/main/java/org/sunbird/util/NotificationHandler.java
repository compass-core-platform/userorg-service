package org.sunbird.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.response.Response;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

public class NotificationHandler implements Runnable {

    private static final LoggerUtil logger = new LoggerUtil(NotificationHandler.class);
    private static final ScheduledExecutorService scheduler = ExecutorManager.getExecutorService();
    private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private final String LEARNING_SERVICE_URL =  "http://learner-service:9000/private/user/feed/v2/create";
    private static final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public void run() {
        try {
            logger.info("NotificationHandler:run:started.");
            fetchAndScheduleNotifications();
        } catch (Exception e) {
            logger.error("Failed to initialize NotificationHandler", e);
        }

    }


        private void fetchAndScheduleNotifications() {
            List<Map<String, Object>> unprocessedNotifications = fetchUnprocessedNotifications();
            LocalDateTime currentDateTime = LocalDateTime.now();
                if (!unprocessedNotifications.isEmpty()) {
                for (Map<String, Object> notification : unprocessedNotifications) {
                    Date scheduleTime = (Date) notification.getOrDefault(JsonKey.SCHEDULE_TIME, null);
                    if (scheduleTime != null) {
                        LocalDateTime scheduleDateTime = scheduleTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                        if (scheduleDateTime.isBefore(currentDateTime) || scheduleDateTime.isEqual(currentDateTime)) {
                            logger.info("Processing notification (ID: " + notification.get("id") + ") scheduled for: " + scheduleTime);
                            processNotification(notification);
                        } else {
                            logger.info("Skipping notification (ID: " + notification.get("id") + ") scheduled for future time: " + scheduleTime);
                        }
                    }
                }
            } else {
                    logger.info("No unprocessed notifications found. All notifications are up to date.");
                }
        }

    private List<Map<String, Object>> fetchUnprocessedNotifications() {
        logger.info("fetchUnprocessedNotifications::started.");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(JsonKey.IS_DELIVERED, Boolean.valueOf(Boolean.FALSE));
        Response response = cassandraOperation.getRecordsByProperties("sunbird_notifications", "schedule_notifications", properties, null);
        logger.info("fetchUnprocessedNotifications::response." + response);
        return (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    }


    public void processNotification(Map<String,Object> notification) {
        try {
            List<Map<String, Object>> dataList = objectMapper.readValue((String) notification.get(JsonKey.DATA), new TypeReference<List<Map<String, Object>>>() {});
            logger.info("user feed dataList :: "+dataList);
            Map<String, List<String>> filters = objectMapper.readValue((String) notification.get(JsonKey.FILTERS), new TypeReference<Map<String, List<String>>>() {});
            logger.info("user feed filters :: "+filters);
            Map<String, Object> request = new HashMap<>();
            request.put(JsonKey.DATA, dataList);
            request.put(JsonKey.FILTERS, filters);
            request.put(JsonKey.DATAVALUE,notification.getOrDefault(JsonKey.DATAVALUE,""));
            request.put(JsonKey.NOTIFICATIONID,notification.getOrDefault(JsonKey.ID,""));
            Map<String, Object> requestWrapper = new HashMap<>();
            requestWrapper.put("request", request);
            logger.info("user feed request ::: "+requestWrapper);
            String requestJson = objectMapper.writeValueAsString(requestWrapper);
            logger.info("user feed payload :: "+requestJson);
            String response = HttpClientUtil.post(LEARNING_SERVICE_URL,requestJson,null,null);
            logger.info("response "+response);


        } catch (JsonMappingException e) {
            logger.error(null,"JsonMappingException ",e);
        } catch (JsonProcessingException e) {
            logger.error(null,"JsonProcessingException ",e);
        }
    }

}
