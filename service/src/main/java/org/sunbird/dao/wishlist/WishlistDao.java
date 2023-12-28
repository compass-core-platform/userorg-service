package org.sunbird.dao.wishlist;

import org.sunbird.model.wishlist.Wishlist;
import org.sunbird.request.Request;
import org.sunbird.response.Response;

public interface WishlistDao {

    Response getWishlistById(Request request);

    Response addToWishlist(Request request);

    Response removeFromWishlist(Request request);
}
