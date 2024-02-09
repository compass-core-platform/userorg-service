package org.sunbird.service.feed.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections.CollectionUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sunbird.client.NotificationServiceClient;
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

public class FeedServiceImpl implements IFeedService {
  private final LoggerUtil logger = new LoggerUtil(FeedServiceImpl.class);
  private final ObjectMapper mapper = new ObjectMapper();
  private NotificationServiceClient serviceClient;

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
    String body = buildUserSearchRequestBody(requestData);
    String URL = learner_BASE_URL + USER_SEARCH_URL;
    String userNames = HttpClientUtil.post(URL,body,null,context);
    logger.info("printing userNames  "+userNames);
    List<String> mailIds = extractUserNames(userNames);
    logger.info("printing mailIds  "+mailIds);
    //ToDo for testing purpose after testing will remove
    List<String> emailIds = List.of("anilkumar.kammalapalli@tarento.com");
    Map<String, Object> notification = buildNotification(requestData, emailIds);
    Request newRequest = buildRequest(notification);

    logger.info(context, "FeedServiceImpl:NOTIFICATIONS: " + notification);

    return serviceClient.sendSyncV2NotificationV2(newRequest, context);
  }


  private Map<String, Object> buildNotification(Map<String, Object> requestData, List<String> emailIds) {
    Map<String, Object> data = (Map<String, Object>) requestData.get(JsonKey.DATA);
    Map<String, Object> action = (Map<String, Object>) data.get(JsonKey.ACTION);
    Map<String, Object> template = (Map<String, Object>) action.get(JsonKey.TEMPLATE);
    Map<String, Object> templateConfig = (Map<String, Object>) template.get(JsonKey.CONFIG);
    Map<String, Object> params = (Map<String, Object>) template.get(JsonKey.PARAMS);

    Map<String, Object> newTemplate = new HashMap<>();
    newTemplate.put(JsonKey.CONFIG, templateConfig);
    newTemplate.put(JsonKey.TYPE, template.get(JsonKey.TYPE));
    newTemplate.put(JsonKey.DATA, template.get(JsonKey.DATA));
    newTemplate.put(JsonKey.ID, template.get(JsonKey.ID));
    newTemplate.put(JsonKey.PARAMS, params);

    Map<String, Object> newAction = new HashMap<>();
    newAction.put(JsonKey.TEMPLATE, newTemplate);
    newAction.put(JsonKey.TYPE, action.get(JsonKey.TYPE));
    newAction.put(JsonKey.CATEGORY, action.get(JsonKey.CATEGORY));
    newAction.put(JsonKey.CREATED_BY, action.get(JsonKey.CREATED_BY));

    Map<String, Object> notification = new HashMap<>();
    notification.put(JsonKey.TYPE, data.get(JsonKey.TYPE));
    notification.put(JsonKey.PRIORITY, 1);
    notification.put(JsonKey.ACTION, newAction);
    notification.put(JsonKey.IDS, emailIds);

    return notification;
  }


  public String buildUserSearchRequestBody(Map<String, Object> request) {
    try {
      Map<String, Object> requestBody = new HashMap<>();
      Map<String, Object> requestBodyMap = new HashMap<>();
      Map<String, Object> filters = new HashMap<>();

      List<String> roles = Optional.ofNullable((List<String>) request.get(JsonKey.ROLES)).orElse(Collections.emptyList());
      List<String> designation = Optional.ofNullable((List<String>) request.get(JsonKey.DESIGNATION)).orElse(Collections.emptyList());
      List<String> taxonomyCategory1 = Optional.ofNullable((List<String>) request.get(JsonKey.TAXONOMYCATEGORYONE)).orElse(Collections.emptyList());
      List<String> taxonomyCategory2 = Optional.ofNullable((List<String>) request.get(JsonKey.TAXONOMYCATEGORYTWO)).orElse(Collections.emptyList());
      List<String> taxonomyCategory3 = Optional.ofNullable((List<String>) request.get(JsonKey.TAXONOMYCATEGORYTHREE)).orElse(Collections.emptyList());
      List<String> taxonomyCategory4 = Optional.ofNullable((List<String>) request.get(JsonKey.TAXONOMYCATEGORYFOUR)).orElse(Collections.emptyList());
      List<String> taxonomyCategory5 = Optional.ofNullable((List<String>) request.get(JsonKey.TAXONOMYCATEGORYFIVE)).orElse(Collections.emptyList());
      List<String> frameworkIds = Optional.ofNullable((List<String>) request.get(JsonKey.FRAMEWORKIDS)).orElse(Collections.emptyList());


      if (!roles.isEmpty()) {
        filters.put(JsonKey.ROLES_ROLE, roles);
      }
      if (!designation.isEmpty()) {
        filters.put(JsonKey.PROFILEDETAILS_PROFESSIONALDETAILS_DESIGNATION, designation);
      }
      if (!taxonomyCategory1.isEmpty()) {
        filters.put(JsonKey.FRAMEWORK_TAXONOMYCATEGORY1, taxonomyCategory1);
      }
      if (!taxonomyCategory2.isEmpty()) {
        filters.put(JsonKey.FRAMEWORK_TAXONOMYCATEGORY2, taxonomyCategory2);
      }
      if (!taxonomyCategory3.isEmpty()) {
        filters.put(JsonKey.FRAMEWORK_TAXONOMYCATEGORY3, taxonomyCategory3);
      }
      if (!taxonomyCategory4.isEmpty()) {
        filters.put(JsonKey.FRAMEWORK_TAXONOMYCATEGORY4, taxonomyCategory4);
      }
      if (!taxonomyCategory5.isEmpty()) {
        filters.put(JsonKey.FRAMEWORK_TAXONOMYCATEGORY5, taxonomyCategory5);
      }
      if (!frameworkIds.isEmpty()) {
        filters.put(JsonKey.FRAMEWORK_ID,frameworkIds);
      }

      // Construct request body
      requestBodyMap.put(JsonKey.FILTERS, filters);
      requestBodyMap.put(JsonKey.FIELDS,Arrays.asList(JsonKey.USERNAME));
      requestBody.put(JsonKey.REQUEST, requestBodyMap);

      return mapper.writeValueAsString(requestBody);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  public  List<String> extractUserNames(String responseString) {

    if (responseString == null || responseString.isEmpty()) {
      return Collections.emptyList();
    }

    JSONObject jsonResponse = new JSONObject(responseString);
    JSONArray contentArray = jsonResponse.getJSONObject(JsonKey.RESULT).getJSONObject(JsonKey.RESPONSE).getJSONArray(JsonKey.CONTENT);

    return contentArray.toList().stream()
            .map(obj -> (JSONObject) obj)
            .map(obj -> obj.getString(JsonKey.USERNAME))
            .collect(Collectors.toList());
  }

  private Request buildRequest(Map<String, Object> notification) {
    Map<String, Object> reqObj = new HashMap<>();
    reqObj.put("notifications", Arrays.asList(notification));

    Request newRequest = new Request();
    newRequest.setRequest(reqObj);

    return newRequest;
  }

}
