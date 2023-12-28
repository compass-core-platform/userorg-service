package controllers.wishlist;

import akka.actor.ActorRef;
import controllers.BaseController;
import org.sunbird.logging.LoggerUtil;
import org.sunbird.operations.ActorOperations;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CompletionStage;

public class WishlistController extends BaseController {
    private final LoggerUtil logger = new LoggerUtil(WishlistController.class);

    @Inject
    @Named("user_wishlist_actor")
    private ActorRef userWishlistActor;

    public CompletionStage<Result> getUserWishlist(Http.Request httpRequest) {
        logger.info("Get wishlist controller " + httpRequest.body().asJson().get("request").toString());
        return handleRequest(
                userWishlistActor,
                ActorOperations.GET_USER_WISHLIST.getValue(),
                httpRequest.body().asJson(),
                httpRequest);
    }

    public CompletionStage<Result> addToWishlist(Http.Request httpRequest) {
        logger.info("Add wishlist controller " + httpRequest.body().asJson().get("request").toString());
        return handleRequest(
                userWishlistActor,
                ActorOperations.ADD_USER_WISHLIST.getValue(),
                httpRequest.body().asJson(),
                httpRequest);
    }

    public CompletionStage<Result> removeFromWishlist(Http.Request httpRequest) {
        logger.info("Remove wishlist controller " + httpRequest.body().asJson().get("request").toString());
        return handleRequest(
                userWishlistActor,
                ActorOperations.REMOVE_USER_WISHLIST.getValue(),
                httpRequest.body().asJson(),
                httpRequest);
    }


}
