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

  public Response insertV1(Request request, RequestContext context) {
    logger.info(context, "FeedServiceImpl:insert method called");

    ObjectMapper objectMapper = new ObjectMapper();

    Map<String, Object> requestData = (Map<String, Object>) request.getRequest();

    List<String> roles = Optional.ofNullable((List<String>) requestData.get("roles")).orElse(Collections.emptyList());

    List<String> designations = Optional.ofNullable((List<String>) requestData.get("designations")).orElse(Collections.emptyList());

    List<List<String>> taxonomyCategories = IntStream.rangeClosed(1, 5)  // Stream of integers from 1 to 5
            .mapToObj(i -> Optional.ofNullable((List<String>) requestData.get("taxonomyCategory" + i)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

//    List<String> userIds = getUserIds(roles, designations, taxonomyCategories);

    List<String> userIds = Arrays.asList("anilkumar.kammalapalli@tarento.com");

    System.out.println("printing data "+request.get("data"));

    Map<String, Object> notification = new HashMap<>();
    notification.put("type", "email");
    notification.put("priority", 1);
    Map<String, Object> action = new HashMap<>();
    Map<String, Object> template = new HashMap<>();
    Map<String, Object> config = new HashMap<>();
    config.put("sender", "support@igot-dev.in");
    config.put("subject", "New Content Request for Capacity Building Plan Development");
    template.put("config", config);
    template.put("type", "email");
    template.put("data", request.get("data"));
    template.put("id", "cbplanContentRequestTemplate");
    Map<String, Object> params = new HashMap<>();
    params.put("competency_subtheme", "Design Thinking, PEST (Political, Economic, Social, Technological) Consciousness, Research & Need Analysis.");
    params.put("orgName", "Ministry for Testing");
    params.put("mdo_name", "Ministry for Testing");
    params.put("name", "Ministry for Testing");
    params.put("description", "The content should be simple but exhaustive and should containe all the details mentioned above should be simple and easy to understand");
    params.put("competency_area", "Functional");
    params.put("competency_theme", "Citizen Centricity, Policy Architecture.");
    params.put("fromEmail", "support@igot-dev.in");
    template.put("params", params);
    action.put("template", template);
    action.put("type", "email");
    action.put("category", "email");
    Map<String, Object> createdBy = new HashMap<>();
    createdBy.put("id", "userID");
    createdBy.put("type", "user");
    action.put("createdBy", createdBy);
    notification.put("action", action);
    notification.put("ids", userIds);

    // Create the request object
    Map<String, Object> reqObj = new HashMap<>();
    reqObj.put(JsonKey.NOTIFICATIONS, Arrays.asList(notification));
    Request req = new Request();
    req.setRequest(reqObj);

    logger.info(context, "FeedServiceImpl:NOTIFICATIONS: " + reqObj);

    return serviceClient.sendSyncV2Notification(req, context);
  }


  private List<String> getUserIds(List<String> roles, List<String> designations, List<List<String>> taxonomyCategories) {

    return Arrays.asList("user1", "user2", "user3");
  }

}
