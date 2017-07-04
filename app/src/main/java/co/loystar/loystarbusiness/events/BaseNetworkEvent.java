package co.loystar.loystarbusiness.events;

/**
 * Created by laudbruce-tagoe on 4/8/17.
 */

class BaseNetworkEvent {
    static final String UNHANDLED_MSG = "UNHANDLED_MSG";
    static final int UNHANDLED_CODE = -1;

    static class OnStart<Rq> {
        private Rq mRequest;

        OnStart(Rq request) {
            mRequest = request;
        }

        public Rq getRequest() {
            return mRequest;
        }
    }

    static class OnDone<Rs> {

        private Rs mResponse;

        OnDone(Rs response) {
            mResponse = response;
        }

        public Rs getResponse() {
            return mResponse;
        }

    }

    static class OnFailed {

        private String mErrorMessage;
        private int mCode;

        OnFailed(String errorMessage, int code) {
            mErrorMessage = errorMessage;
            mCode = code;
        }

        public String getErrorMessage() {
            return mErrorMessage;
        }

        public int getCode() {
            return mCode;
        }

    }
}
