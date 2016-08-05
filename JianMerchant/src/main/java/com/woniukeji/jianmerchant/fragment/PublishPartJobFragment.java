package com.woniukeji.jianmerchant.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.woniukeji.jianmerchant.R;
import com.woniukeji.jianmerchant.adapter.JobsAdapter;
import com.woniukeji.jianmerchant.adapter.MyTypeAdapter;
import com.woniukeji.jianmerchant.adapter.RegionAdapter;
import com.woniukeji.jianmerchant.base.BaseFragment;
import com.woniukeji.jianmerchant.base.Constants;
import com.woniukeji.jianmerchant.entity.BaseBean;
import com.woniukeji.jianmerchant.entity.CityAndCategoryBean;
import com.woniukeji.jianmerchant.entity.Model;
import com.woniukeji.jianmerchant.entity.RegionBean;
import com.woniukeji.jianmerchant.entity.TypeBean;
import com.woniukeji.jianmerchant.http.HttpMethods;
import com.woniukeji.jianmerchant.http.ProgressSubscriber;
import com.woniukeji.jianmerchant.publish.HistoryJobAdapter;
import com.woniukeji.jianmerchant.publish.PublishDetailActivity;
import com.woniukeji.jianmerchant.utils.DateUtils;
import com.woniukeji.jianmerchant.utils.LogUtils;
import com.woniukeji.jianmerchant.utils.SPUtils;
import com.woniukeji.jianmerchant.widget.FixedRecyclerView;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscriber;

public class PublishPartJobFragment extends BaseFragment implements View.OnClickListener {

    private static final String PARAM = "type";
    private static final int FIRST = 0;
    @InjectView(R.id.recycler_region)
    RecyclerView recyclerRegion;
    @InjectView(R.id.recycler_type)
    RecyclerView recyclerType;
    @InjectView(R.id.recycler_jobs)
    RecyclerView recyclerJobs;
    @InjectView(R.id.next_page)
    TextView nextPage;
    //设置此fragment的参数，创建新兼职，历史纪录，模板
    private String type;
    private List<RegionBean> dataSetRegion = Arrays.asList(new RegionBean("全国", 0), new RegionBean("三亚", 1), new RegionBean("海口", 2), new RegionBean("北京", 3), new RegionBean("西安", 4), new RegionBean("杭州", 5));
    private List<TypeBean> dataSetType = Arrays.asList(new TypeBean("短期", 0), new TypeBean("长期", 1), new TypeBean("实习生", 2), new TypeBean("寒/暑假工", 3));
    BaseBean<List<RegionBean>> regionBaseBean = new BaseBean<>();
    BaseBean<List<TypeBean>> typeBaseBean = new BaseBean<>();
    BaseBean<List<CityAndCategoryBean.ListTTypeBean>> categoryBean = new BaseBean<>();
    //next step Activity
    private Intent intent;
    private Bundle bundle = new Bundle();
    ;
    private boolean isFirst = true;
    private SwipeRefreshLayout refreshLayout;
    private FixedRecyclerView list;
    /**
     * 历史兼职信息集合
     */
    private List<Model.ListTJobEntity> modelList=new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private int lastVisibleItem;
    private int checkOutPage = 0;
    private int checkOutPage1 = 0;

    private HistoryJobAdapter historyJobAdapter;
    private Handler mHandler = new Handler(){
        SoftReference<Context> reference = new SoftReference<Context>(getHoldingContext());

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FIRST:
                    if (type.equals("mb")) {
                        final String only = DateUtils.getDateTimeToOnly(System.currentTimeMillis());
                        historyJobAdapter = new HistoryJobAdapter(modelList, getHoldingContext(), "1", new HistoryJobAdapter.deleteCallBack() {
                            @Override
                            public void deleOnClick(int job_id, int merchant_id, final int position) {
                                HttpMethods.getInstance().deleteModelInfo(new Subscriber<BaseBean>() {
                                    @Override
                                    public void onCompleted() {

                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        Toast.makeText(getHoldingContext(), "删除失败了。。。", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onNext(BaseBean baseBean) {
                                        modelList.remove(position);
                                        historyJobAdapter.notifyDataSetChanged();
                                        Toast.makeText(getHoldingContext(), baseBean.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }, only,String.valueOf(merchant_id), String.valueOf(job_id));
                            }
                        });
                    } else {
                        historyJobAdapter = new HistoryJobAdapter(modelList, getHoldingContext(), "0", new HistoryJobAdapter.deleteCallBack() {
                            @Override
                            public void deleOnClick(int job_id, int merchant_id, int position) {
                                //删除模板
                            }
                        });
                    }
                    list.setAdapter(historyJobAdapter);
                    break;

            }
        }
    };
    private boolean isCanLoadDate =true;


    public static PublishPartJobFragment newInstance(String type) {
        PublishPartJobFragment partJobFragment = new PublishPartJobFragment();
        Bundle bundle = new Bundle();
        bundle.putString(PARAM, type);
        partJobFragment.setArguments(bundle);
        return partJobFragment;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            type = bundle.getString(PARAM);
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (type.equals("cjxjz")) {
            View view = inflater.inflate(R.layout.fragment_create_partjob, container, false);
            ButterKnife.inject(this, view);

            GridLayoutManager regionGridManager = new GridLayoutManager(getHoldingContext(), 4);
            recyclerRegion.setLayoutManager(regionGridManager);
            recyclerRegion.setItemAnimator(new DefaultItemAnimator());
            regionBaseBean.setData(dataSetRegion);
            RegionAdapter adapterRegion = new RegionAdapter(regionBaseBean, getHoldingContext());
            recyclerRegion.setAdapter(adapterRegion);

            GridLayoutManager typeGridManager = new GridLayoutManager(getHoldingContext(), 4);
            recyclerType.setLayoutManager(typeGridManager);
            recyclerType.setItemAnimator(new DefaultItemAnimator());
            typeBaseBean.setData(dataSetType);
            MyTypeAdapter typeAdapter = new MyTypeAdapter(typeBaseBean, getHoldingContext());
            recyclerType.setAdapter(typeAdapter);

            GridLayoutManager jobsGridManager = new GridLayoutManager(getHoldingContext(), 4);
            recyclerJobs.setLayoutManager(jobsGridManager);
            recyclerJobs.setItemAnimator(new DefaultItemAnimator());


            //访问网络+设置recyclerjobs的数据
            getCategoryToBean();
            nextPage.setOnClickListener(this);


            return view;

        } else if (type.equals("lsjl")) {
            View view = inflater.inflate(R.layout.activity_history_job, container, false);
            initHistoryView(view);
            refreshLayout.setRefreshing(false);
            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refreshLayout.setRefreshing(false);
                }
            });
            linearLayoutManager = new LinearLayoutManager(getHoldingContext());
            list.setLayoutManager(linearLayoutManager);
            list.setItemAnimator(new DefaultItemAnimator());
            list.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (modelList.size() > 5 && lastVisibleItem == modelList.size()) {
                        if (isCanLoadDate) {
                            getHistroyJobs(++checkOutPage);
                        }
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                }
            });
            //访问网络获取数据
            getHistroyJobs(checkOutPage);

            return view;


        } else if (type.equals("mb")) {
            View view = inflater.inflate(R.layout.activity_history_job, container, false);
            initHistoryView(view);
            refreshLayout.setRefreshing(false);
            refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refreshLayout.setRefreshing(false);
                }
            });
            linearLayoutManager = new LinearLayoutManager(getHoldingContext());
            list.setLayoutManager(linearLayoutManager);
            list.setItemAnimator(new DefaultItemAnimator());
            list.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (modelList.size() > 5 && lastVisibleItem == modelList.size()) {
                        if (isCanLoadDate) {
                            getHistroyJobs(++checkOutPage);
                        }
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                }
            });
            //访问网络获取数据
            getHistroyJobs(checkOutPage);
            return view;
        }
        TextView view = new TextView(getHoldingContext());
        view.setText(type);
        return view;


    }

    private void initHistoryView(View view) {
        refreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        list = (FixedRecyclerView) view.findViewById(R.id.list);

    }

    /**
     * 从服务器获取历史模板兼职
     */
    private void getHistroyJobs(int pagecount) {
        final int count = 10 * pagecount;
        String only = DateUtils.getDateTimeToOnly(System.currentTimeMillis());
        final int merchantid= (int) SPUtils.getParam(getHoldingContext(),Constants.USER_INFO,Constants.USER_MERCHANT_ID,0);
        ProgressSubscriber.SubscriberOnNextListenner<Model> listenner = new ProgressSubscriber.SubscriberOnNextListenner<Model>() {
            @Override
            public void onNext(Model model) {
                List<Model.ListTJobEntity> list_t_job = model.getList_t_job();
                if (list_t_job != null && list_t_job.size()>0) {
                    modelList.addAll(list_t_job);
                }
                if (count==0) {//第一次
                    mHandler.sendEmptyMessage(FIRST);
                    isFirst = false;
                } else if (list_t_job != null && list_t_job.size() > 0) {//有数据
                    historyJobAdapter.notifyDataSetChanged();
                } else {//没数据
                    isCanLoadDate = false;
                }
            }
        };
        ProgressSubscriber<Model> modelProgressSubscriber = new ProgressSubscriber<Model>(listenner,getHoldingContext());
        //0代表第一页，10代表第二页，20代表第三页
        if (type.equals("mb")) {
            HttpMethods.getInstance().getHistroyJobFromServer(modelProgressSubscriber, only, String.valueOf(merchantid), "1", String.valueOf(count));
        } else {
            HttpMethods.getInstance().getHistroyJobFromServer(modelProgressSubscriber,only,String.valueOf(merchantid),"0",String.valueOf(count));
        }

    }


    /**
     * 访问网络获取兼职类别
     */
    private void getCategoryToBean() {
        ProgressSubscriber.SubscriberOnNextListenner<CityAndCategoryBean> onNextListenner = new ProgressSubscriber.SubscriberOnNextListenner<CityAndCategoryBean>() {

            @Override
            public void onNext(CityAndCategoryBean cityAndCategoryBean) {
                List<CityAndCategoryBean.ListTTypeBean> typeList = cityAndCategoryBean.getList_t_type();
                categoryBean.setData(typeList);
                JobsAdapter jobsAdapter = new JobsAdapter(categoryBean, getHoldingContext());
                recyclerJobs.setAdapter(jobsAdapter);
                bundle.putParcelable("CityAndCategoryBean", cityAndCategoryBean);
            }
        };

        String only = DateUtils.getDateTimeToOnly(System.currentTimeMillis());
        int loginId = (int) SPUtils.getParam(getHoldingContext(), Constants.LOGIN_INFO, Constants.SP_USERID, 0);
        HttpMethods.getInstance().getCityAndCategory(new ProgressSubscriber<CityAndCategoryBean>(onNextListenner, getHoldingContext()), only, String.valueOf(loginId));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        saveStateToArguments();
        ButterKnife.reset(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next_page:
                if (CheckAndGet()) {
                    startActivity(intent);
                }
                break;
        }
    }

    /**
     * true 地区，类型，岗位都选择了一项。
     *
     * @return
     */
    private boolean CheckAndGet() {
//        intent = new Intent(getHoldingContext(), TestActivity.class);
        intent = new Intent(getHoldingContext(), PublishDetailActivity.class);
        intent.setAction("fromFragment");


        for (int i = 0; i < regionBaseBean.getData().size(); i++) {
            LogUtils.i("regionBaseBean", regionBaseBean.getData().get(i).toString());
            if (regionBaseBean.getData().get(i).isSelect()) {
                bundle.putString("region", regionBaseBean.getData().get(i).getRegion());
                bundle.putInt("region_id", regionBaseBean.getData().get(i).getId());
                break;
            } else if (i == regionBaseBean.getData().size() - 1) {
                Toast.makeText(getHoldingContext(), "请选择地区", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        for (int i = 0; i < typeBaseBean.getData().size(); i++) {
            LogUtils.i("typeBaseBean", typeBaseBean.getData().get(i).toString());
            if (typeBaseBean.getData().get(i).isSelect()) {
                bundle.putString("type", typeBaseBean.getData().get(i).getType());
                bundle.putInt("type_id", typeBaseBean.getData().get(i).getId());
                break;
            } else if (i == typeBaseBean.getData().size() - 1) {
                Toast.makeText(getHoldingContext(), "请选择类型", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        for (int i = 0; i < categoryBean.getData().size(); i++) {
            LogUtils.i("categoryBean", categoryBean.getData().get(i).toString());
            if (categoryBean.getData().get(i).isSelect()) {
                bundle.putString("category", categoryBean.getData().get(i).getType_name());
                bundle.putInt("category_id", categoryBean.getData().get(i).getId());
                break;
            } else if (i == categoryBean.getData().size() - 1) {
                Toast.makeText(getHoldingContext(), "请选择岗位", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        intent.putExtras(bundle);
        return true;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        if (isFirst) {
//            isFirst = false;
//        } else {
//            restoreStateFromArguments();
//        }
    }

//    private void restoreStateFromArguments() {
//        Bundle bundleRestore = getArguments();
//        if (bundleRestore != null) {
//            ArrayList<RegionBean> regionList = bundleRestore.getParcelableArrayList("region");
//            regionBaseBean.setData(regionList);
//            ArrayList<TypeBean> typeList =bundleRestore.getParcelableArrayList("type");
//            typeBaseBean.setData(typeList);
//        }
//    }
//
//    private void saveStateToArguments() {
//        Bundle bundleSave = getArguments();
//        bundleSave.putParcelableArrayList("region", (ArrayList<? extends Parcelable>) regionBaseBean.getData());
//        bundleSave.putParcelableArrayList("type", (ArrayList<? extends Parcelable>) typeBaseBean.getData());
//    }

}
