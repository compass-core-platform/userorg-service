package org.sunbird.service.feed.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections.CollectionUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sunbird.client.NotificationServiceClient;
import org.sunbird.dao.notification.ScheduleNotificationDao;
import org.sunbird.dao.notification.impl.ScheduleNotificationDaoImpl;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
import org.sunbird.http.HttpClientUtil;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.model.user.Feed;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.IFeedService;
import org.sunbird.service.user.UserService;
import org.sunbird.service.user.impl.UserServiceImpl;

public class FeedServiceImpl implements IFeedService {
  private final LoggerUtil logger = new LoggerUtil(FeedServiceImpl.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private NotificationServiceClient serviceClient;
  private final ScheduleNotificationDao scheduleNotificationDao = ScheduleNotificationDaoImpl.getInstance();
  ObjectMapper objectMapper = new ObjectMapper();

  private String learner_BASE_URL = "http://learner-service:9000";
  private String USER_SEARCH_URL = "/private/user/v1/search";
  public FeedServiceImpl(NotificationServiceClient serviceClient){
     this.serviceClient= serviceClient;
  }
  @Override
  public Response insert(Request request, RequestContext context) {

    logger.info(context, "FeedServiceImpl:insert method called : ");

    Request req = new Request();
    Map<String, Object> reqObj = new HashMap<>();
    reqObj.put(JsonKey.NOTIFICATIONS, Arrays.asList(request.getRequest()));
    req.setRequest(reqObj);
    logger.info(context, "FeedServiceImpl:NOTIFICATIONS: "+reqObj);
    return serviceClient.sendSyncV2Notification(req,context);
  }

  @Override
  public Response update(Request request, RequestContext context) {
    logger.debug(context, "FeedServiceImpl:update method called : ");
    return serviceClient.updateV1Notification(request,context);
  }

  @Override
  public List<Feed> getFeedsByProperties(Map<String, Object> request, RequestContext context) {

    List<Feed> feedList = new ArrayList<>();
    Request req = new Request();
    req.setRequest(request);
    Response response = serviceClient.readV1Notification(req,context);
    if (null != response) {
      List<Map<String, Object>> feeds =
          (List<Map<String, Object>>) response.getResult().get(JsonKey.FEEDS);
      if (CollectionUtils.isNotEmpty(feeds)) {
         for (Map<String, Object> feed : feeds) {
           feedList.add(mapper.convertValue(feed, Feed.class));
         }
       }
     }

    return feedList;
  }

  @Override
  public void delete(Request request, RequestContext context) {
    Request req = new Request();
    Map<String,Object> reqObj = new HashMap<>();
    reqObj.put(JsonKey.IDS,Arrays.asList(request.getRequest().get(JsonKey.FEED_ID)));
    reqObj.put(JsonKey.USER_ID,request.getRequest().get(JsonKey.USER_ID));
    reqObj.put(JsonKey.CATEGORY,request.getRequest().get(JsonKey.CATEGORY));
    req.setRequest(reqObj);
    Response response = serviceClient.deleteV1Notification(req,context);
    if(null == response){
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
  }

  @Override
  public Response insertV1(Request request, RequestContext context) {
    logger.info(context, "FeedServiceImpl:insert method called");

    Map<String, Object> requestData = (Map<String, Object>) request.getRequest();
    boolean isScheduled = (boolean) requestData.getOrDefault("isScheduleNotification", false);

    if (isScheduled) {
     return handleScheduledNotification(requestData, context);
    } else {
     return handleImmediateNotification(requestData, context);
    }
  }

  private Response handleScheduledNotification(Map<String, Object> requestData, RequestContext context) {
    logger.info("handleScheduledNotification started :: "+requestData);
    try {
      String scheduleTime = (String) requestData.getOrDefault("scheduleTime", ZonedDateTime.now().toString());
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSSXX");
      Instant instant = Instant.from(formatter.parse(scheduleTime));
      Date date = Date.from(instant);

      List<Map<String, Object>> dataList = (List<Map<String, Object>>) requestData.get(JsonKey.DATA);
      Map<String, Object> requestFilters = (Map<String, Object>) requestData.get("filters");

      requestData.put("is_delivered", Boolean.FALSE);
      requestData.put("data", objectMapper.writeValueAsString(dataList));
      requestData.put("filters", objectMapper.writeValueAsString(requestFilters));
      requestData.put("scheduleTime", date);
      requestData.put("id", UUID.randomUUID().toString());

      String audience = (String) requestData.getOrDefault("audience", "");
      String title = (String) requestData.getOrDefault("title", "");

    } catch (Exception e) {
      logger.error(context, "Error occurred while processing scheduled notification: {}", e);
      ProjectCommonException.throwServerErrorException(ResponseCode.SERVER_ERROR);
    }
    return scheduleNotificationDao.saveScheduledNotificationDetails(requestData, context);
  }



  private Response handleImmediateNotification(Map<String, Object> requestData, RequestContext context) {
    logger.info("handleImmediateNotification started :: "+requestData);
    String body = buildUserSearchRequestBody(requestData);
    logger.info("user search request body :: " + body);

    String URL = learner_BASE_URL + USER_SEARCH_URL;
    String userNames = HttpClientUtil.post(URL, body, null, context);
    logger.info("printing userNames  " + userNames);

    List<String> mailIds = extractUserNames(userNames);
    List<String> userIds = extractUserIds(userNames);
    logger.info("printing mailIds  " + mailIds);
    logger.info("printing userIds  " + userIds);

    List<Map<String, Object>> notifications = buildNotification(requestData, mailIds);
    Request newRequest = buildRequest(notifications);

    logger.info(context, "FeedServiceImpl:NOTIFICATIONS: " + notifications);
    Response response = feedNotification(requestData, userIds, context);
    logger.info(context, "FeedServiceImpl:feedNotification:result: " + response.getResult());
    return serviceClient.sendSyncV2NotificationV2(newRequest, context);
  }


  private List<Map<String, Object>> buildNotification(Map<String, Object> requestData, List<String> emailIds) {

    //Map<String, Object> data = (Map<String, Object>) requestData.get(JsonKey.DATA);
    List<Map<String, Object>> dataList = (List<Map<String, Object>>) requestData.get(JsonKey.DATA);
    List<Map<String, Object>> notificationList = new ArrayList<>();
    logger.info("size of the data "+dataList.size());
    for (Map<String, Object> data:dataList) {
      Map<String, Object> action = (Map<String, Object>) data.get(JsonKey.ACTION);
      Map<String, Object> template = (Map<String, Object>) action.get(JsonKey.TEMPLATE);
      Map<String, Object> templateConfig = (Map<String, Object>) template.get(JsonKey.CONFIG);
      Map<String, Object> params = (Map<String, Object>) template.get(JsonKey.PARAMS);
      if (params == null || params.isEmpty()) {
        params = new HashMap<>();
      }

      Map<String, Object> newTemplate = new HashMap<>();
      newTemplate.put(JsonKey.CONFIG, templateConfig);
      newTemplate.put(JsonKey.TYPE, template.get(JsonKey.TYPE));
      newTemplate.put(JsonKey.DATA, template.get(JsonKey.DATA));
      newTemplate.put(JsonKey.ID, template.get(JsonKey.ID));
      newTemplate.put(JsonKey.NOTIFICATIONID, requestData.getOrDefault(JsonKey.NOTIFICATIONID,""));
      params.put(JsonKey.FROM_EMAIL, "gohila.mariappan@tarento.com");
      newTemplate.put(JsonKey.PARAMS, params);

      Map<String, Object> newAction = new HashMap<>();
      newAction.put(JsonKey.TEMPLATE, newTemplate);
      newAction.put(JsonKey.TYPE, action.get(JsonKey.TYPE));
      newAction.put(JsonKey.CATEGORY, action.get(JsonKey.CATEGORY));
      Map<String, Object> created_BY = (Map<String, Object>) action.get(JsonKey.CREATED_BY);
      if (created_BY == null || created_BY.isEmpty()) {
        created_BY = new HashMap<>();
        created_BY.put(JsonKey.TYPE, JsonKey.USER);
        created_BY.put(JsonKey.ID, JsonKey.USERID);
      } else {
        created_BY.put(JsonKey.ID, JsonKey.USERID);
      }
      newAction.put(JsonKey.CREATED_BY, created_BY);

      Map<String, Object> notification = new HashMap<>();
      notification.put(JsonKey.TYPE, data.get(JsonKey.TYPE));
      notification.put(JsonKey.PRIORITY, data.get(JsonKey.PRIORITY));
      notification.put(JsonKey.ACTION, newAction);
      notification.put(JsonKey.IDS, emailIds);
      notificationList.add(notification);
    }
    return notificationList;
  }

  public String buildUserSearchRequestBody(Map<String, Object> request) {
    try {
      Map<String, Object> requestBody = new HashMap<>();
      Map<String, Object> requestBodyMap = new HashMap<>();
      Map<String, Object> filters = new HashMap<>();

      Map<String, Object> requestFilters = (Map<String, Object>) request.get("filters");
      if (requestFilters != null) {
        for (Map.Entry<String, Object> entry : requestFilters.entrySet()) {
          Object value = entry.getValue();
          if (isValidFilterValue(value)) {
            filters.put(entry.getKey(), value);
          }
        }
      }
      requestBodyMap.put(JsonKey.FILTERS, filters);
      requestBodyMap.put(JsonKey.FIELDS, List.of(JsonKey.USERNAME,JsonKey.USER_ID));
      requestBody.put(JsonKey.REQUEST, requestBodyMap);

      return mapper.writeValueAsString(requestBody);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  private boolean isValidFilterValue(Object value) {
    if (value instanceof List) {
      List<?> list = (List<?>) value;
      return !list.isEmpty();
    } else if (value instanceof String) {
      String str = (String) value;
      return !str.isEmpty();
    } else if (value instanceof Number) {
      return true;
    }
    return false;
  }

  public static List<String> extractUserIds(String responseString) {
    if (responseString == null || responseString.isEmpty()) {
      return Collections.emptyList();
    }

    JSONArray contentArray = new JSONObject(responseString)
            .getJSONObject("result")
            .getJSONObject("response")
            .getJSONArray("content");

    return IntStream.range(0, contentArray.length())
            .mapToObj(index -> contentArray.getJSONObject(index).optString("userId"))
            .collect(Collectors.toList());
  }
  public  List<String> extractUserNames(String responseString) {

    if (responseString == null || responseString.isEmpty()) {
      return Collections.emptyList();
    }

    JSONObject jsonResponse = new JSONObject(responseString);
    JSONArray contentArray = jsonResponse.getJSONObject(JsonKey.RESULT).getJSONObject(JsonKey.RESPONSE).getJSONArray(JsonKey.CONTENT);

    return contentArray.toList().stream()
            .map(obj -> (Map<String, Object>) obj)
            .map(obj -> (String) obj.get(JsonKey.USERNAME))
            .collect(Collectors.toList());
  }

  private Request buildRequest(List<Map<String, Object>> notifications) {
    Map<String, Object> reqObj = new HashMap<>();
//    reqObj.put("notifications", Arrays.asList(notification));
    reqObj.put("notifications", notifications);

    Request newRequest = new Request();
    newRequest.setRequest(reqObj);

    return newRequest;
  }

  public Response feedNotification(Map<String,Object> notification, List<String> userIds, RequestContext context) {
    logger.info(context, "feedNotification:NOTIFICATIONS: data "+notification);
    logger.info(context, "feedNotification:userIds: "+userIds);

    List<Map<String, Object>> dataList = (List<Map<String, Object>>) notification.get(JsonKey.DATA);
    List<Map<String, Object>> notificationList = new ArrayList<>();
    logger.info("size of the data "+dataList.size());
    String subject = null;
      for (Map<String, Object> data:dataList) {
        Map<String, Object> action = (Map<String, Object>) data.get(JsonKey.ACTION);
        Map<String, Object> template = (Map<String, Object>) action.get(JsonKey.TEMPLATE);
        Map<String, Object> templateConfig = (Map<String, Object>) template.get(JsonKey.CONFIG);
        subject = (String) templateConfig.getOrDefault("subject", "You have new notification");
      }


    Request req = new Request();
    Map<String,Object> notifications = new HashMap<>();
    Map<String, Object> reqObj = new HashMap<>();
    Map<String, Object> dataMap = new HashMap<>();

    reqObj.put("userId", userIds);
    reqObj.put(JsonKey.CATEGORY, JsonKey.USER_FEED_DB);
    reqObj.put(JsonKey.PRIORITY, 1);


    Map<String, Object> dataObject = new HashMap<>();
    dataObject.put("type", 1); // Type 1
    Map<String, Object> actionData = new HashMap<>();
    actionData.put("actionType", JsonKey.USER_FEED_DB);
//    actionData.put("title", templateConfig.getOrDefault("subject","You have new notification"));
    actionData.put("title", subject);
    actionData.put("description", notification.getOrDefault("dataValue",""));
    dataObject.put("actionData", actionData);

    reqObj.put(JsonKey.DATA, dataObject);

    // Add notifications to the request
    notifications.put(JsonKey.NOTIFICATIONS, Arrays.asList(reqObj));
    req.setRequest(notifications);

    logger.info(context, "feedNotification:NOTIFICATIONS: "+reqObj);
    return serviceClient.sendSyncV2Notification(req, context);
  }


}
