//package org.sunbird.dao.wishlist.impl;
//
//import org.sunbird.dao.wishlist.WishlistDao;
//import org.sunbird.model.wishlist.Wishlist;
//import org.sunbird.request.Request;
//
//import java.util.Arrays;
//
//public final class WishlistDaoImpl implements WishlistDao {
//
//    private static WishlistDaoImpl instance;
//
//    private WishlistDaoImpl() {}
//
//    public static WishlistDao getInstance() {
//        if (instance == null) {
//            instance = new WishlistDaoImpl();
//        }
//        return instance;
//    }
//
//    @Override
//    public Wishlist getWishlistById(Request request) {
//        Wishlist wishlist = new Wishlist();
//        wishlist.setUserId("user1");
//        wishlist.setCourseList(Arrays.asList("ABDC", "sdfsdfs", "dfsdfs"));
//        return wishlist;
//    }
//}

package org.sunbird.dao.wishlist.impl;

import org.sunbird.cassandra.CassandraOperation;
import org.sunbird.dao.wishlist.WishlistDao;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.keys.JsonKey;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.util.ProjectUtil;

import java.util.*;

public final class WishlistDaoImpl implements WishlistDao {
    private static final String KEY_SPACE_NAME = ProjectUtil.getConfigValue(JsonKey.SUNBIRD_KEYSPACE);
    private static WishlistDaoImpl instance;
    private final CassandraOperation cassandraOperation = ServiceFactory.getInstance();
    private final LoggerUtil logger = new LoggerUtil(WishlistDaoImpl.class);
    private WishlistDaoImpl() {
    }

    public static WishlistDao getInstance() {
        if (instance == null) {
            instance = new WishlistDaoImpl();
        }
        return instance;
    }

    @Override
    public Response getWishlistById(Request request) {
        Response response = new Response();
        logger.info("Get wishlist DaoImpl " + request.getRequest());
        String userId = request.getRequest().get(JsonKey.USER_ID).toString();
        List<String> list = new ArrayList<>();
        list.add(JsonKey.COURSE_IDS);
        Response response1 = cassandraOperation.getRecordById(KEY_SPACE_NAME, JsonKey.WISHLIST, userId, list, null);
        List<Map<String, Object>> responseList = (List<Map<String, Object>>) response1.getResult().get(JsonKey.RESPONSE);
        logger.info("Get response list : "+ responseList);
        if (responseList.isEmpty()) {
            response.put(JsonKey.WISHLIST, Collections.emptyList());
            return response;
        } else {
            response.put(JsonKey.WISHLIST, responseList.get(0).get(JsonKey.COURSE_IDS));
            return response;
        }

    }

    public Response addToWishlist(Request request) {
        String userId = request.getRequest().get(JsonKey.USER_ID).toString();
        String courseId = request.getRequest().get(JsonKey.COURSE_ID).toString();
        logger.info("Add wishlist list : "+ courseId);

        if (userId.isBlank() || courseId.isBlank()) {
            Response response = new Response();
            response.put("message", "user id or course id missing");
            return response;
        } else {
            // Check if the course already exists in the wishlist
            Response wishlistResponse = getWishlistById(request);
            List<String> existingCourses = (List<String>) wishlistResponse.getResult().get(JsonKey.WISHLIST);
            logger.info(" existingCourses: "+ existingCourses);

            if (existingCourses.contains(courseId)) {
                Response response = new Response();
                response.put("message", "Course already exists in the wishlist");
                return response;
            } else {
                Map<String, Object> wishlistRecord = new HashMap<>();
                wishlistRecord.put(JsonKey.ID, userId);
                wishlistRecord.put(JsonKey.COURSE_IDS, Arrays.asList(courseId));
                Response updateResponse = cassandraOperation.updateRecord(KEY_SPACE_NAME, JsonKey.WISHLIST, JsonKey.COURSE_IDS, userId, courseId, true, null);
                Response response = new Response();
                response.put("message", "Course added to the wishlist");
                return response;
            }
        }
    }


    public Response removeFromWishlist(Request request) {
        String userId = request.getRequest().get(JsonKey.USER_ID).toString();
        String courseId = request.getRequest().get(JsonKey.COURSE_ID).toString();
        if (userId.isBlank() || courseId.isBlank()) {
            Response response = new Response();
            response.put("message : ", "user id or course id missing");
            return response;
        } else {
            Map<String, Object> wishlistRecord = new HashMap<>();
            wishlistRecord.put(JsonKey.ID, userId);
            wishlistRecord.put(JsonKey.COURSE_IDS, Arrays.asList(courseId));
            Response response = cassandraOperation.updateRecord(KEY_SPACE_NAME, JsonKey.WISHLIST, JsonKey.COURSE_IDS, userId, courseId, false, null);
            return response;
        }
    }

}

