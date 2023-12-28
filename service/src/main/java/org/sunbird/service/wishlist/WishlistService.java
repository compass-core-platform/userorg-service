package org.sunbird.service.wishlist;

import org.sunbird.request.Request;
import org.sunbird.response.Response;

public interface WishlistService {
    Response getWishlistByUserId(Request request);

    Response addToWishlist(Request request);

    Response removeFromWishlist(Request request);

}
