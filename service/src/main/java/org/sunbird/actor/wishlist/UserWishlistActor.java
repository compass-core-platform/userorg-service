package org.sunbird.actor.wishlist;

import org.sunbird.actor.core.BaseActor;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.request.Request;
import org.sunbird.request.RequestContext;
import org.sunbird.response.Response;
import org.sunbird.service.wishlist.WishlistService;
import org.sunbird.service.wishlist.impl.WishlistServiceImpl;

public class UserWishlistActor extends BaseActor {

    private final WishlistService wishlistService = WishlistServiceImpl.getInstance();
    private final LoggerUtil logger = new LoggerUtil(UserWishlistActor.class);

    @Override
    public void onReceive(Request request) throws Throwable {
        String operation = request.getOperation();
        RequestContext context = request.getRequestContext();
        logger.info("Wishlist actor request : " + request.getOperation());
        logger.info("Wishlist actor context : " + request.getRequestContext());
        switch (operation) {
            case "getUserWishlist":
                getUserWishlist(request);
                break;
            case "addUserWishlist":
                addToUserWishlist(request);
                break;
            case "removeUserWishlist":
                removeFromWishlist(request);
                break;
            default:
                onReceiveUnsupportedOperation();
        }
    }

    private void getUserWishlist(Request request) {
        Response response = wishlistService.getWishlistByUserId(request);
        logger.info("Get wishlist actor response : " + response);
        sender().tell(response, self());
    }

    private void addToUserWishlist(Request request) {
        Response response = wishlistService.addToWishlist(request);
        logger.info("Add wishlist actor response : " + response);
        sender().tell(response, self());
    }

    private void removeFromWishlist(Request request) {
        Response response = wishlistService.removeFromWishlist(request);
        logger.info("Remove wishlist actor response : " + response);
        sender().tell(response, self());
    }

}
