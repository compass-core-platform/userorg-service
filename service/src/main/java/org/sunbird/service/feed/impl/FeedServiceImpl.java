package org.sunbird.service.feed.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections.CollectionUtils;

import org.sunbird.client.NotificationServiceClient;
import org.sunbird.exception.ProjectCommonException;
import org.sunbird.exception.ResponseCode;
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

    ObjectMapper objectMapper = new ObjectMapper();

    Map<String, Object> requestData = (Map<String, Object>) request.getRequest();
    Map<String, Object> data = (Map<String, Object>) requestData.get("data");

    List<String> roles = Optional.ofNullable((List<String>) request.getRequest().get("roles")).orElse(Collections.emptyList());

    List<List<String>> taxonomyCategories = IntStream.rangeClosed(1, 5)
            .mapToObj(i -> Optional.ofNullable((List<String>) requestData.get("taxonomyCategory" + i)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    List<String> emailIds = (List<String>) data.get("ids");

    Map<String, Object> notification = new HashMap<>();
    notification.put("type", data.get("type"));
    notification.put("priority", 1);

    Map<String, Object> action = (Map<String, Object>) data.get("action");
    Map<String, Object> template = (Map<String, Object>) action.get("template");
    Map<String, Object> templateConfig = (Map<String, Object>) template.get("config");
    Map<String, Object> params = (Map<String, Object>) template.get("params");

    Map<String, Object> newTemplate = new HashMap<>();
    newTemplate.put("config", templateConfig);
    newTemplate.put("type", template.get("type"));
    newTemplate.put("data", template.get("data"));
    newTemplate.put("id", template.get("id"));
    newTemplate.put("params", params);

    Map<String, Object> newAction = new HashMap<>();
    newAction.put("template", newTemplate);
    newAction.put("type", action.get("type"));
    newAction.put("category", action.get("category"));
    newAction.put("createdBy", action.get("createdBy"));

    notification.put("action", newAction);
    notification.put("ids", emailIds);

    Map<String, Object> reqObj = new HashMap<>();
    reqObj.put("notifications", Arrays.asList(notification));

    Request newRequest = new Request();
    newRequest.setRequest(reqObj);

    logger.info(context, "FeedServiceImpl:NOTIFICATIONS: " + reqObj);

    return serviceClient.sendSyncV2NotificationV2(newRequest, context);
  }




  private List<String> getUserIds(List<String> roles, List<String> designations, List<List<String>> taxonomyCategories) {

    return Arrays.asList("user1", "user2", "user3");
  }

}
