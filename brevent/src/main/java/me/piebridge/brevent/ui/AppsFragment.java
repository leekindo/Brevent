package me.piebridge.brevent.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/1/25.
 */
public abstract class AppsFragment extends Fragment {

    private static final String PACKAGE_FRAMEWORK = "android";

    private View mView;

    private RecyclerView mRecycler;

    private AppsItemAdapter mAdapter;

    private AppsItemHandler mHandler;

    private boolean mResumed;

    private boolean mVisible;

    private boolean mLoaded;

    private volatile boolean mExpired;

    private Signature[] frameworkSignatures;

    private static boolean warned;

    private SharedPreferences preferences;

    private long lastSync;

    private SwipeRefreshLayout mRefresh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_brevent, container, false);
            mRecycler = mView.findViewById(R.id.recycler);
            mRefresh = mView.findViewById(R.id.refresh);
        }
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mResumed = true;
        lazyLoad();
        if (mLoaded) {
            mHandler.sendEmptyMessage(AppsItemHandler.MSG_UPDATE_ITEM);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mVisible = isVisibleToUser;
        lazyLoad();
    }

    private void lazyLoad() {
        if (mVisible && mResumed && mRecycler != null) {
            if (mRecycler.getAdapter() == null) {
                BreventActivity activity = (BreventActivity) getActivity();
                if (activity != null && !activity.isStopped()) {
                    activity.showFragmentAsync(this, 0);
                }
            } else if (mExpired) {
                mExpired = false;
                mAdapter.retrievePackagesAsync();
            }
        }
    }

    @Override
    public void onStop() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mAdapter = null;
        mHandler = null;
        mRecycler = null;
        mView = null;
        super.onDestroy();
    }

    public abstract boolean accept(PackageManager packageManager, PackageInfo packageInfo);

    public void refresh() {
        if (mAdapter != null) {
            mAdapter.updateAppsInfo();
        }
        if (mRefresh != null) {
            mRefresh.setRefreshing(false);
        }
    }

    public void setRefreshEnabled(boolean enabled) {
        if (mRefresh != null) {
            mRefresh.setEnabled(enabled);
        }
    }

    @UiThread
    public void show() {
        if (mHandler == null) {
            mHandler = new AppsItemHandler(this, mRecycler);
        } else {
            mHandler.obtainMessage();
        }
        mRecycler.addOnScrollListener(new AppsScrollListener(mHandler));
        mRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecycler.addItemDecoration(new DividerItemDecoration(mRecycler.getContext(),
                LinearLayoutManager.VERTICAL));
        if (mAdapter == null) {
            mAdapter = new AppsItemAdapter(this, mHandler);
            if (mRefresh != null) {
                mRefresh.setOnRefreshListener((BreventActivity) getActivity());
            }
        }
        mRecycler.setAdapter(mAdapter);
        mLoaded = true;
    }

    public void update(Collection<String> packageNames) {
        BreventActivity activity = (BreventActivity) getActivity();
        if (mAdapter != null && activity != null) {
            List<AppsInfo> appsInfoList = mAdapter.getAppsInfo();
            int size = appsInfoList.size();
            for (int i = 0; i < size; ++i) {
                AppsInfo info = appsInfoList.get(i);
                if (info.isPackage() && packageNames.contains(info.packageName)) {
                    mAdapter.notifyItemChanged(i);
                }
            }
            activity.setSelectCount(mAdapter.getSelectedSize());
        }
    }

    public void select(Collection<String> packageNames) {
        if (mAdapter != null) {
            update(mAdapter.select(packageNames));
        }
    }

    public void clearSelected() {
        if (mAdapter != null) {
            update(mAdapter.clearSelected());
        }
    }

    public int getSelectedSize() {
        if (mAdapter == null) {
            return 0;
        } else {
            return mAdapter.getSelectedSize();
        }
    }

    public void selectInverse() {
        if (mAdapter != null) {
            update(mAdapter.selectInverse());
        }
    }

    public void retrievePackages() {
        if (mAdapter != null) {
            mAdapter.retrievePackages();
        }
    }

    public Collection<String> getSelected() {
        if (mAdapter != null) {
            return mAdapter.getSelected();
        } else {
            return Collections.emptySet();
        }
    }

    protected final boolean isFrameworkPackage(PackageManager packageManager,
                                               PackageInfo packageInfo) {
        String packageName = packageInfo.packageName;
        if (packageManager.checkSignatures(PACKAGE_FRAMEWORK, BuildConfig.APPLICATION_ID) !=
                PackageManager.SIGNATURE_MATCH) {
            return packageManager.checkSignatures(PACKAGE_FRAMEWORK, packageName) ==
                    PackageManager.SIGNATURE_MATCH;
        } else {
            if (preferences == null) {
                if (!warned) {
                    warned = true;
                    UILog.w(BuildConfig.APPLICATION_ID + " shouldn't be framework app");
                }
                Context context = getActivity();
                if (context == null) {
                    return isFrameworkPackage(packageManager, packageName);
                } else {
                    preferences = context.getSharedPreferences("signature", Context.MODE_PRIVATE);
                    lastSync = AppsLabelLoader.getLastSync(context);
                }
            }
            if (preferences.contains(packageName) && packageInfo.lastUpdateTime <= lastSync) {
                return preferences.getBoolean(packageName, false);
            }
            boolean signature = isFrameworkPackage(packageManager, packageName);
            preferences.edit().putBoolean(packageName, signature).apply();
            return signature;
        }
    }

    private boolean isFrameworkPackage(PackageManager packageManager, String packageName) {
        boolean frameworkApp = Arrays.equals(getFrameworkSignatures(packageManager),
                BreventActivity.getSignatures(packageManager, packageName));
        UILog.i("checking framework app for " + packageName + ": " + (frameworkApp ? "yes" : "no"));
        return frameworkApp;
    }

    private Signature[] getFrameworkSignatures(PackageManager packageManager) {
        if (frameworkSignatures == null) {
            synchronized (this) {
                if (frameworkSignatures == null) {
                    frameworkSignatures = BreventActivity.getSignatures(packageManager,
                            PACKAGE_FRAMEWORK);
                }
            }
        }
        return frameworkSignatures;
    }

    public boolean supportAllApps() {
        return false;
    }

    public void setExpired() {
        if (mAdapter != null) {
            mExpired = true;
            lazyLoad();
        }
    }

    public final boolean isImportant(String packageName) {
        return ((BreventActivity) getActivity()).isImportant(packageName);
    }

    public final boolean isGcm(String packageName) {
        return ((BreventActivity) getActivity()).isGcm(packageName);
    }

    public final boolean isFavorite(String packageName) {
        return ((BreventActivity) getActivity()).isFavorite(packageName);
    }

    public final String getLabel(String label, String packageName) {
        return ((BreventActivity) getActivity()).getLabel(label, packageName);
    }

}
