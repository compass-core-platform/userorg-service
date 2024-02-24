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

        for (Map<String, Object> notification : unprocessedNotifications) {
            Date scheduleTime = (Date) notification.getOrDefault("scheduletime", null);
            if (scheduleTime != null) {
                LocalDateTime scheduleDateTime = scheduleTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                if (scheduleDateTime.isBefore(currentDateTime) || scheduleDateTime.isEqual(currentDateTime)) {
                        processNotification(notification);
                } else {
                    logger.info("Skipping notification with schedule time in the future: " + scheduleTime);
                }
            }
        }
    }

    private List<Map<String, Object>> fetchUnprocessedNotifications() {
        logger.info("fetchUnprocessedNotifications::started.");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("is_delivered", Boolean.valueOf(Boolean.FALSE));
        Response response = cassandraOperation.getRecordsByProperties("sunbird_notifications", "schedule_notifications", properties, null);
        logger.info("fetchUnprocessedNotifications::response." + response);
        return (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
    }


    public void processNotification(Map<String,Object> notification) {
        try {
            List<Map<String, Object>> dataList = objectMapper.readValue((String) notification.get("data"), new TypeReference<List<Map<String, Object>>>() {});

            Map<String, List<String>> filters = objectMapper.readValue((String) notification.get("filters"), new TypeReference<Map<String, List<String>>>() {});

            Map<String, Object> request = new HashMap<>();
            request.put("data", dataList);
            request.put("filters", filters);
            request.put("dataValue",notification.getOrDefault("dataValue",""));
            request.put("notificationId",notification.getOrDefault("id",""));
            String requestJson = objectMapper.writeValueAsString(request);
            String response = HttpClientUtil.post(LEARNING_SERVICE_URL,requestJson,null,null);
            logger.info("response "+response);


        } catch (JsonMappingException e) {
            logger.error(null,"JsonMappingException ",e);
        } catch (JsonProcessingException e) {
            logger.error(null,"JsonProcessingException ",e);
        }
    }

}
