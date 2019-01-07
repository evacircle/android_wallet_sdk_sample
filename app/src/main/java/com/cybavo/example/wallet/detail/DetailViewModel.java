package com.cybavo.example.wallet.detail;

import android.app.Application;

import com.cybavo.example.wallet.helper.Helpers;
import com.cybavo.wallet.service.api.Callback;
import com.cybavo.wallet.service.wallet.Wallet;
import com.cybavo.wallet.service.wallet.Wallets;
import com.cybavo.wallet.service.wallet.results.GetHistoryResult;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class DetailViewModel extends AndroidViewModel {

    public static class Factory implements ViewModelProvider.Factory {

        private final Application mApp;
        private final Wallet mWallet;

        public Factory(Application application, Wallet wallet) {
            mApp = application;
            mWallet = wallet;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new DetailViewModel(mApp, mWallet);
        }
    }
    private final static int HISTORY_BATCH_COUNT = 10;

    private final Wallet mWallet;
    private Wallets mService = Wallets.getInstance();

    private MutableLiveData<HistoryList> mHistory;

    public DetailViewModel(@NonNull Application application, Wallet wallet) {
        super(application);
        mWallet = wallet;
        mHistory = new MutableLiveData<>();
        mHistory.setValue(new HistoryList());
    }

    public LiveData<HistoryList> getHistory() {
        if (!mHistory.getValue().init) {
            fetchHistory(0);
        }
        return mHistory;
    }

    public void fetchHistory(int start) {

        // skip loading
        if (mHistory.getValue().loading) {
            return;
        }
        // skip no more
        if (!mHistory.getValue().hasMore) {
            return;
        }
        updateHistory(true, null);

        final int count = HISTORY_BATCH_COUNT;
//        mService.getHistory(60, "", "0xf6DabB290FCE73f5617ED381ca90dBb7af0E8295",
        mService.getHistory(mWallet.currency, mWallet.tokenAddress, mWallet.address,
                start, count, new Callback<GetHistoryResult>() {
                    @Override
                    public void onError(Throwable error) {
                        Helpers.showToast(getApplication(), "getHistory failed: " + error.getMessage());
                    }

                    @Override
                    public void onResult(GetHistoryResult result) {
                        updateHistory(false, result);
                    }
                });
    }

    public void refreshHistory() {
        mHistory.setValue(new HistoryList());
        fetchHistory(0);
    }

    private void updateHistory(boolean loading, GetHistoryResult result) {
        HistoryList newHistory = new HistoryList();
        newHistory.loading = loading;

        final HistoryList oldHistory = mHistory.getValue();
        if (oldHistory != null) {
            newHistory.history.addAll(oldHistory.history);
            newHistory.init = oldHistory.init;
        }
        if (result != null) {
            newHistory.history.addAll(Arrays.asList(result.transactions));
            if (newHistory.history.size() >= result.totalCount) {
                newHistory.hasMore = false;
            }
            newHistory.init = true;
        }

        mHistory.setValue(newHistory);
    }
}