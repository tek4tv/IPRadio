//package vn.tek4tv.radioip.signalR;
//
//
//import android.util.Log;
//
//
//import com.smartarmenia.dotnetcoresignalrclientjava.HubConnection;
//import com.smartarmenia.dotnetcoresignalrclientjava.HubConnectionListener;
//import com.smartarmenia.dotnetcoresignalrclientjava.HubEventListener;
//import com.smartarmenia.dotnetcoresignalrclientjava.HubMessage;
//import com.smartarmenia.dotnetcoresignalrclientjava.WebSocketHubConnectionP2;
//
//
//
//public class SignalrService {
//    private static final String TAG = SignalrService.class.getSimpleName();
//
//    private HubConnection hubConnection;
//
//
//
//
//    public void connect(String token) {
//
//        hubConnection = new WebSocketHubConnectionP2("http://103.233.49.15:2666/gameShowHub", "Bearer  " + token);
//
//        Log.i(TAG, "=>>>>>>>>>>>> Begin connect to Hub");
//
//        hubConnection.addListener(new HubConnectionListener() {
//            @Override
//            public void onConnected() {
//                Log.i(TAG, "onConnected");
//            }
//
//            @Override
//            public void onDisconnected() {
//                Log.i(TAG, "onDisconnected");
//            }
//
//            @Override
//            public void onMessage(HubMessage message) {
//                Log.d(TAG, "onMessage: " + message.toString());
//            }
//
//            @Override
//            public void onError(Exception exception) {
//                Log.i(TAG, "onError", exception);
//            }
//
//
//        });
//
//        hubConnection.subscribeToEvent("UpdateInfo", new HubEventListener() {
//            @Override
//            public void onEventMessage(HubMessage message) {
//                Log.i(TAG, "onEventMessage: " + message.toString());
//            }
//        });
//        hubConnection.subscribeToEvent("StartGame", new HubEventListener() {
//            @Override
//            public void onEventMessage(HubMessage message) {
//                Log.i(TAG, "onMessage: getTarget = " + message.getTarget());
//                Log.i(TAG, "onMessage: getInvocationId = " + message.getInvocationId());
//                Log.i(TAG, "onMessage: message.getArguments()[0].toString() = " + message.getArguments()[0].toString());
//            }
//        });
//
//        hubConnection.subscribeToEvent("QuestionReceive", new HubEventListener() {
//            @Override
//            public void onEventMessage(HubMessage message) {
//                if (listener != null) {
//                    String format = StringEscapeUtils.unescapeHtml3(message.getArguments()[1].getAsString());
//                    String idQuestion = message.getArguments()[0].getAsString();
//                    int time = Integer.parseInt(message.getArguments()[2].getAsString());
//                    listener.onMessage(format, idQuestion, time);
//                }
//
//            }
//        });
//
//        hubConnection.subscribeToEvent("UpdateUserOnline", new HubEventListener() {
//            @Override
//            public void onEventMessage(HubMessage message) {
//
//            }
//        });
//
//        hubConnection.subscribeToEvent("UpdateUserAnswer", new HubEventListener() {
//            @Override
//            public void onEventMessage(HubMessage message) {
//                Log.d(TAG, "UpdateUserAnswer " + message.getArguments()[0].toString());
//            }
//        });
//
//
//        hubConnection.subscribeToEvent("OnConnected", new HubEventListener() {
//            @Override
//            public void onEventMessage(HubMessage message) {
//                joinGameShow(codeGameShow);
//            }
//        });
//
//        hubConnection.subscribeToEvent("EndGame", new HubEventListener() {
//            @Override
//            public void onEventMessage(HubMessage message) {
//                Log.i(TAG, "onMessage: getTarget = " + message.getTarget());
//                Log.i(TAG, "onMessage: getInvocationId = " + message.getInvocationId());
//                Log.i(TAG, "onMessage: message.getArguments()[0].toString() = " + message.getArguments()[0].toString());
//            }
//        });
//
//        hubConnection.subscribeToEvent("FinishQuestion", new HubEventListener() {
//            @Override
//            public void onEventMessage(HubMessage message) {
//                String answer = message.getArguments()[0].toString();
//                String idQuestion = message.getArguments()[1].toString();
//            }
//        });
//
//        hubConnection.subscribeToEvent("EndQuestionAnswer", new HubEventListener() {
//            @Override
//            public void onEventMessage(HubMessage message) {
//                String result = message.getArguments()[0].toString();
//                Log.d(TAG, "EndQuestionAnswer result = " + result);
//            }
//        });
//
//        hubConnection.connect();
//    }
//
//    public void stop() {
//        if (hubConnection != null){
//            hubConnection.disconnect();
//        }
//    }
//
//    public void joinGameShow(String gameShowCode) {
//        if (hubConnection != null && hubConnection.isConnected()) {
//            hubConnection.invoke("JoinGameShow", gameShowCode);
//        }
//    }
//
//    public void logoutGameShow(String gameShowCode) {
//        if (hubConnection != null && hubConnection.isConnected()) {
//            hubConnection.invoke("LogoutGameShow", gameShowCode);
//        }
//    }
//
//    public void answerQuestion(String gameShowCode, String idQuestion, String answer) {
//        if (hubConnection != null && hubConnection.isConnected()) {
//            hubConnection.invoke("AnswerQuestion", gameShowCode, idQuestion, answer);
//        }
//    }
//}
