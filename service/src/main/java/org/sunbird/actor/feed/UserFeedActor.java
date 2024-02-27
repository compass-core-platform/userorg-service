package org.sunbird.actor.feed;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.*;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.client.NotificationServiceClient;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.model.user.Feed;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.feed.FeedFactory;
import org.sunbird.service.feed.IFeedService;
import org.sunbird.telemetry.dto.TelemetryEnvKey;
import org.sunbird.util.Util;

public class UserFeedActor extends BaseActor {

  private IFeedService feedService;

  private final ObjectMapper mapper = new ObjectMapper();

  private CassandraOperation cassandraOperation = ServiceFactory.getInstance();
  @Override
  public void onReceive(Request request) throws Throwable {
    Util.initializeContext(request, TelemetryEnvKey.USER);
    RequestContext context = request.getRequestContext();
    String operation = request.getOperation();
    logger.debug(context, "UserFeedActor:onReceive called for operation : " + operation);
    NotificationServiceClient serviceClient = new NotificationServiceClient();
    feedService = FeedFactory.getInstance(serviceClient);
    switch (operation) {
      case "getUserFeedById":
        String userId = (String) request.getRequest().get(JsonKey.USER_ID);
        getUserFeed(userId, context);
        break;
      case "createUserFeed":
        createUserFeed(request, context);
        break;
      case "deleteUserFeed":
        deleteUserFeed(request, context);
        break;
      case "updateUserFeed":
        updateUserFeed(request, context);
        break;
      case "createUserFeedV2":
        createUserFeedV2(request, context);
        break;
      case "getNotificationList":
        String deliverd = (String) request.getRequest().get("delivered");
        fetchNotifications(deliverd, context);
        break;
      default:
        onReceiveUnsupportedOperation();
    }
  }

  private void getUserFeed(String userId, RequestContext context) {
    Map<String, Object> reqMap = new WeakHashMap<>(2);
    reqMap.put(JsonKey.USER_ID, userId);
    List<Feed> feedList = feedService.getFeedsByProperties(reqMap, context);
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.USER_FEED, feedList);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    sender().tell(response, self());
  }

  private void createUserFeed(Request request, RequestContext context) {
    request
        .getRequest()
        .put(JsonKey.CREATED_BY, (String) request.getContext().get(JsonKey.REQUESTED_BY));
    Response feedCreateResponse = feedService.insert(request, context);
    logger.info("feedCreateResponse ::" +feedCreateResponse);
    sender().tell(feedCreateResponse, self());
  }

  private void deleteUserFeed(Request request, RequestContext context) {
    Response feedDeleteResponse = new Response();
    feedService.delete(request, context);
    feedDeleteResponse.getResult().put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    sender().tell(feedDeleteResponse, self());
  }

  private void updateUserFeed(Request request, RequestContext context) {
    Map<String, Object> updateRequest = request.getRequest();
    String feedId = (String) updateRequest.get(JsonKey.FEED_ID);
    updateRequest.put(JsonKey.IDS, Arrays.asList(feedId));
    updateRequest.put(JsonKey.UPDATED_BY, request.getContext().get(JsonKey.REQUESTED_BY));
    Response feedUpdateResponse = feedService.update(request, context);
    sender().tell(feedUpdateResponse, self());
  }

  private void createUserFeedV2(Request request, RequestContext context) throws IOException {
    request
            .getRequest()
            .put(JsonKey.CREATED_BY, (String) request.getContext().get(JsonKey.REQUESTED_BY));
    logger.info("createUserFeedV2 ::" +request);
    Response feedCreateResponse = feedService.insertV1(request, context);
    logger.info("feedCreateResponse ::" +feedCreateResponse);
    sender().tell(feedCreateResponse, self());
  }

  private void fetchNotifications(String value, RequestContext context) {
    List<Map<String, Object>> notificationsList = fetchNotificationsList(value,context);
    List<Map<String, Object>> simplifiedNotifications = new ArrayList<>();
    for (Map<String, Object> notification : notificationsList) {
      Map<String, Object> simplifiedNotification = new HashMap<>();
      simplifiedNotification.put("title",  notification.getOrDefault("title",""));
      simplifiedNotification.put("scheduletime", notification.get("scheduletime"));
      simplifiedNotification.put("audience", notification.getOrDefault("audience",""));
      simplifiedNotifications.add(simplifiedNotification);
    }
    Map<String, Object> result = new HashMap<>();
    result.put(JsonKey.NOTIFICATIONS, simplifiedNotifications);
    Response response = new Response();
    response.put(JsonKey.RESPONSE, result);
    sender().tell(response, self());
  }
  private List<Map<String, Object>> fetchNotificationsList(String value, RequestContext context) {
    logger.info("fetchNotifications::started.");
    boolean isDelivered = Boolean.parseBoolean(value);
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("is_delivered", isDelivered);
    Response response = cassandraOperation.getRecordsByProperties("sunbird_notifications", "schedule_notifications", properties, null);
    logger.info("fetchUnprocessedNotifications::response." + response);
    return (List<Map<String, Object>>) response.get(JsonKey.RESPONSE);
  }
}
