package vn.tek4tv.radioip.signalR;

import android.os.AsyncTask;

import com.microsoft.signalr.HubConnection;

import vn.tek4tv.radioip.model.HubCallBack;

public class HubConnectionTask  extends AsyncTask<HubConnection, Void, String> {
    private HubCallBack hubCallBack;
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
    public HubConnectionTask(HubCallBack callBack){
        this.hubCallBack = callBack;
    }
    @Override
    protected String doInBackground(HubConnection... hubConnections) {
        HubConnection hubConnection = hubConnections[0];
        hubConnection.start().blockingAwait();
        return hubConnection.getConnectionId();
    }

    @Override
    protected void onPostExecute(String aVoid) {
        super.onPostExecute(aVoid);
        hubCallBack.onCallBack(aVoid);
    }
}
