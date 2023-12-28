package org.sunbird.service.wishlist.impl;

import org.sunbird.dao.wishlist.WishlistDao;
import org.sunbird.dao.wishlist.impl.WishlistDaoImpl;
import org.sunbird.model.wishlist.Wishlist;
import org.sunbird.request.Request;
import org.sunbird.response.Response;
import org.sunbird.service.user.UserOrgService;
import org.sunbird.service.user.impl.UserOrgServiceImpl;
import org.sunbird.service.wishlist.WishlistService;

public class WishlistServiceImpl implements WishlistService {
    private static WishlistServiceImpl wishlistService = null;
    private final WishlistDao wishlistDao = WishlistDaoImpl.getInstance();

    public static WishlistService getInstance() {
        if (wishlistService == null) {
            wishlistService = new WishlistServiceImpl();
        }
        return wishlistService;
    }

    public Response getWishlistByUserId(Request request) {
        return wishlistDao.getWishlistById(request);
    }

    public Response addToWishlist(Request request) {
        return wishlistDao.addToWishlist(request);
    }

    public Response removeFromWishlist(Request request) {
        return wishlistDao.removeFromWishlist(request);
    }
}
